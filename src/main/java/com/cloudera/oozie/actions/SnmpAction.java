package com.cloudera.oozie.actions;

import org.apache.oozie.action.ActionExecutor;
import org.apache.oozie.action.ActionExecutorException;
import org.apache.oozie.client.WorkflowAction;
import org.apache.oozie.util.XmlUtils;
import org.jdom.Element;
import org.jdom.Namespace;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

public class SnmpAction extends ActionExecutor {

  private static final String SNMP_ACTION_NS = "uri:oozie:snmp-action:0.1";
  private static final String ACTION_NAME = "snmp";

  public SnmpAction() {
    super(ACTION_NAME);
  }

  @Override
  public void start(Context context,
                    WorkflowAction action) throws ActionExecutorException {
    context.setStartData("-", "-", "-");
    try {
      Element actionXml = XmlUtils.parseXml(action.getConf());
      Namespace ns = Namespace.getNamespace(SNMP_ACTION_NS);

      String targetServer = actionXml.getChildTextTrim("nmsHost", ns);
      int targetPort = Integer.parseInt(actionXml.getChildTextTrim("nmsPort", ns));
      String communityString = actionXml.getChildText("communityString", ns);
      String oid = actionXml.getChildTextTrim("oid", ns);
      String payload = actionXml.getChildText("payload", ns);

      sendSnmpMessage(targetServer, targetPort, communityString, oid, payload);
      context.setExecutionData("OK", null);
    } catch (Exception e) {
      throw convertException(e);
    }
  }

  @Override
  public void end(Context context,
                  WorkflowAction action) throws ActionExecutorException {
    if (action.getExternalStatus().equals("OK")) {
      context.setEndData(WorkflowAction.Status.OK, WorkflowAction.Status.OK.toString());
    } else {
      context.setEndData(WorkflowAction.Status.ERROR, WorkflowAction.Status.ERROR.toString());
    }
  }

  @Override
  public void check(Context context,
                    WorkflowAction action) throws ActionExecutorException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void kill(Context context,
                   WorkflowAction action) throws ActionExecutorException {
    context.setEndData(WorkflowAction.Status.KILLED, "ERROR");
  }

  private static Set<String> COMPLETED_STATUS = new HashSet<String>();
  static {
    COMPLETED_STATUS.add("SUCCEEDED");
    COMPLETED_STATUS.add("KILLED");
    COMPLETED_STATUS.add("FAILED");
    COMPLETED_STATUS.add("FAILED_KILLED");
  }

  @Override
  public boolean isCompleted(String s) {
    return COMPLETED_STATUS.contains(s);
  }

  protected static void sendSnmpMessage(String targetServer, int targetPort, String communityString,
                               String oid, String payload) throws IOException, ParseException {
    // Initialise PDU
    PDU pdu = new PDU();
    pdu.add(new VariableBinding(new OID(oid), new OctetString(payload)));
    pdu.setType(PDU.TRAP);

    // Target server
    CommunityTarget target = new CommunityTarget();
    target.setCommunity(new OctetString(communityString));
    target.setAddress(new UdpAddress(InetAddress.getByName(targetServer), targetPort));
    target.setVersion(SnmpConstants.version2c);

    // Send the trap
    Snmp snmp = null;
    try {
      snmp = new Snmp(new DefaultUdpTransportMapping());
      snmp.send(pdu, target);
    } finally {
      if (null != snmp) {
        snmp.close();
      }
    }
  }

}
