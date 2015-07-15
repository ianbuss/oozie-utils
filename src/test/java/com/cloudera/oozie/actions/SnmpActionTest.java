package com.cloudera.oozie.actions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snmp4j.*;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class SnmpActionTest {

  Snmp snmp;
  Map<String, String> receivedMessages = new HashMap<String, String>();

  @Before
  public void setup() throws Exception {
    UdpAddress udpAddress = new UdpAddress("127.0.0.1/16200");
    TransportMapping transport = new DefaultUdpTransportMapping(udpAddress, true);
    snmp = new Snmp(transport);
    CommandResponder trapPrinter = new CommandResponder() {
      public synchronized void processPdu(CommandResponderEvent e) {
        PDU command = e.getPDU();
        if (command != null) {
          System.out.println(command.toString());
          for (VariableBinding binding : command.getVariableBindings()) {
            receivedMessages.put(binding.getOid().format(), binding.getVariable().toString());
          }
        }
      }
    };
    snmp.addCommandResponder(trapPrinter);
    transport.listen();
  }

  @After
  public void after() throws IOException {
    snmp.close();
    receivedMessages.clear();
  }

  @Test
  public void sendSnmpMessageTest() throws Exception {
    SnmpAction.sendSnmpMessage("127.0.0.1", 16200, "public", "1.2.3.4", "Test Payload");
    Thread.sleep(1000);
    assertTrue(receivedMessages.containsKey("1.2.3.4"));
    assertTrue(receivedMessages.containsValue("Test Payload"));
  }

}
