/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensimkit.utilities;

/**
 *
 * @author ahmedmaawy
 */

import purejavacomm.CommPortIdentifier;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;
import purejavacomm.UnsupportedCommOperationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;
import purejavacomm.SerialPortEventListener;

public class ZTESerialPorts {
    private ArrayList<CommPortIdentifier> serialPorts;
    private ArrayList<String> serialPortList;
    private SerialPort serialPort;
    private boolean connected = false;
    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedReader bufferedReader;
    private PrintStream printStream;
    
    // Returned by the event handler
    public static String serialPortReturnValue;
    
    // Constants
    final char CTRL_Z = (char)26;
    final String CMD_CHECK_CONNECTION = "AT\r\n";
    final String CMD_SET_TEXT_MODE_FORMAT = "AT+CMGF=1\r\n";
    final String CMD_CHAR_SET_PCCP347 = "AT+CSCS=\"PCCP437\"\r\n";
    final String CMD_SELECT_SIM_STORAGE = "AT+CPMS=\"SM\"\r\n";
    final String CMD_READ_ALL_MESSAGES = "AT+CMGL=\"ALL\"\r\n";
    final String CMD_READ_MESSAGE = "AT+CMGR={{ message_index }}\r\n";
    final String CMD_DETAILED_ERRORS = "AT+CMEE=1";
    final String CMD_DELETE_ALL_MESSAGES = "AT+CMGD=1,4\r\n";

    // Full write command
    final String CMD_WRITE_MESSAGE_TO_MEMORY = "AT+CMGW=\"{{ message_contact }}\"\r{{ message }}";

    // Parial write command
    final String CMD_REQUEST_WRITE_MESSAGE = "AT+CMGW=\"{{ message_contact }}\"\r\n";
    final String CMD_DO_WRITE_AFTER_REQUEST = "{{ message }}";
    
    /**
     * Constructor
     */
    
    public ZTESerialPorts()
    {
        serialPortList = new ArrayList<String>();
        serialPorts = new ArrayList<CommPortIdentifier>();
        
        Enumeration portList = CommPortIdentifier.getPortIdentifiers();
        
        while(portList.hasMoreElements())
        {
            CommPortIdentifier cpi = (CommPortIdentifier) portList.nextElement();
            
            if (cpi.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                serialPorts.add(cpi);
                serialPortList.add(cpi.getName());
            } else if (cpi.getPortType() == CommPortIdentifier.PORT_PARALLEL) {
                // Do nothing at the moment
            } else {
                // Unknown port
            }
        }
    }
    
    /**
     * Synchronized variable access
     * 
     * @param value 
     */
    
    public static synchronized void setSerialPortReturnValue(String value)
    {
        serialPortReturnValue = value;
    }
    
    /**
     * Set the serial port parameters
     * 
     * @param speed
     * @param dataBits
     * @param stopBits
     * @param parity
     * @return boolean
     */
    
    public boolean setParameters(long speed, String dataBits, String stopBits, String parity)
    {
        try {
            int speedValue;
            int dataBitValue;
            int stopBitValue;
            int parityValue;
            
            // Baud rate
            
            if(speed == 75)
            {
                speedValue = 75;
            }
            else if(speed == 110)
            {
                speedValue = 110;
            }
            else if(speed == 300)
            {
                speedValue = 300;
            }
            else if(speed == 1200)
            {
                speedValue = 1200;
            }
            else if(speed == 4800)
            {
                speedValue = 4800;
            }
            else if(speed == 19200)
            {
                speedValue = 19200;
            }
            else if(speed == 38400)
            {
                speedValue = 38400;
            }
            else if(speed == 57600)
            {
                speedValue = 57600;
            }
            else if(speed == 115200)
            {
                speedValue = 115200;
            }
            else {
                speedValue = 9600;
            }
            
            // Data bits
            
            if(dataBits.equals("5"))
            {
                dataBitValue = SerialPort.DATABITS_5;
            }
            else if(dataBits.equals("6"))
            {
                dataBitValue = SerialPort.DATABITS_6;
            }
            else if(dataBits.equals("7"))
            {
                dataBitValue = SerialPort.DATABITS_7;
            }
            else 
            {
                dataBitValue = SerialPort.DATABITS_8;
            }
            
            // Stop bits
            
            if(stopBits.equals("1.5"))
            {
                stopBitValue = SerialPort.STOPBITS_1_5;
            }
            else if(stopBits.equals("2"))
            {
                stopBitValue = SerialPort.STOPBITS_2;
            }
            else {
                stopBitValue = SerialPort.STOPBITS_1;
            }
            
            // Parity
            
            if(parity.equals("Mark"))
            {
                parityValue = SerialPort.PARITY_MARK;
            }
            else if(parity.equals("Odd"))
            {
                parityValue = SerialPort.PARITY_ODD;
            }
            else if(parity.equals("Even"))
            {
                parityValue = SerialPort.PARITY_EVEN;
            }
            else if(parity.equals("Space"))
            {
                parityValue = SerialPort.PARITY_SPACE;
            }
            else {
                parityValue = SerialPort.PARITY_NONE;
            }
            
            serialPort.setSerialPortParams(speedValue, dataBitValue, stopBitValue, parityValue);
        } catch (UnsupportedCommOperationException ex) {
            Logger.getLogger(ZTESerialPorts.class.getName()).log(Level.SEVERE, null, ex);
            
            return false;
        }
        
        return true;
    }
    
    /**
     * Connect to a serial port
     * 
     * @param portIndex
     * 
     * @return boolean
     */
    
    public boolean connectPort(int portIndex)
    {
        if(!connected) {
            try {
                serialPort = (SerialPort)serialPorts.get(portIndex).open("OpenSIMKit", 2000);
                try {
                    
                    serialPort.setSerialPortParams(9600, 
                            SerialPort.DATABITS_8, 
                            SerialPort.STOPBITS_1, 
                            SerialPort.PARITY_NONE);
                    
                    serialPort.addEventListener((SerialPortEventListener)new ZTESerialPortListener());
                    serialPort.notifyOnDataAvailable(true);
                    serialPort.notifyOnOutputEmpty(true);
                    
                    inputStream = serialPort.getInputStream();
                    outputStream = serialPort.getOutputStream();
                    
                    bufferedReader = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
                    printStream = new PrintStream(serialPort.getOutputStream(), true);
                    
                    setTextFormat();
                    setMemSIMCard();
                } catch (UnsupportedCommOperationException ex) {
                    connected = false;
                    Logger.getLogger(ZTESerialPorts.class.getName()).log(Level.SEVERE, null, ex);
                    
                    return false;
                } catch (TooManyListenersException ex) {
                    connected = false;
                    Logger.getLogger(ZTESerialPorts.class.getName()).log(Level.SEVERE, null, ex);
                    
                    return false;
                    
                } catch (IOException ex) {
                    connected = false;
                    Logger.getLogger(ZTESerialPorts.class.getName()).log(Level.SEVERE, null, ex);
                    
                    return false;
                }
            } catch (PortInUseException ex) {
                connected = false;
                Logger.getLogger(ZTESerialPorts.class.getName()).log(Level.SEVERE, null, ex);

                return false;
            }

            connected = true;
        }
        else {
            return false;
        }
        
        return true;
    }
    
    /**
     * Disconnect the port
     * 
     * @return boolean
     */
    
    public boolean disconnectPort()
    {   
        if(connected) {
            new Thread(){
                @Override
                public void run()
                {
                    try {
                        serialPort.getInputStream().close();
                        serialPort.getOutputStream().close();
                    }
                    catch(IOException ex) {
                        Logger.getLogger(ZTESerialPorts.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    serialPort.removeEventListener();
                    serialPort.close();
                }
            }.start();
        }
        
        connected = false;
        
        return true;
    }
    
    private String readPort()
    {
        String output = "";
        String readLine = "";
        
        try {
            while((readLine = bufferedReader.readLine()) != null) {
                output.concat("\r\n" + readLine);
            }
        } catch (IOException ex) {
            Logger.getLogger(ZTESerialPorts.class.getName()).log(Level.SEVERE, null, ex);
            
            return null;
        }
        
        return output;
    }
    
    /**
     * Sets the format to text format
     */
    
    public boolean setTextFormat()
    {
        printStream.print(CMD_SET_TEXT_MODE_FORMAT);
        
        return true;
    }
    
    /**
     * Sets the storage to SIM card storage
     */
    
    public boolean setMemSIMCard()
    {
        printStream.print(CMD_SELECT_SIM_STORAGE);
        
        return true;
    }
    
    /**
     * Runs a custom command
     * 
     * @param command
     * @return 
     */
    
    public String runCommand(String command)
    {
        serialPortReturnValue = "";
        printStream.print(command);
        
        return waitForOutput();
    }
    
    /**
     * Wait for output
     */
    
    private String waitForOutput()
    {
        int numAttempts = 5;
        int currentAttempt = 1;
        
        try {
            while(serialPortReturnValue.trim().equals("") && currentAttempt < numAttempts) {
                Thread.sleep(500);
                currentAttempt ++;
            }
            
            if(currentAttempt == numAttempts && serialPortReturnValue.trim().equals(""))
            {
                return null;
            }  
        } catch (InterruptedException ex) {
            Logger.getLogger(ZTESerialPorts.class.getName()).log(Level.SEVERE, null, ex);
            
            return null;
        }
        
        return serialPortReturnValue.trim();
    }
    
    /**
     * Get all messages in the system
     * 
     * @return String
     */
    
    public String getAllMessages()
    {
        runCommand(CMD_READ_ALL_MESSAGES);
        
        String returnValue = waitForOutput();
        
        return returnValue;
    }
    
    /**
     * Save a message
     * 
     * @param message
     * @return 
     */
    
    public boolean saveMessage(String contact, String message)
    {
        String requestWriteCommand = CMD_REQUEST_WRITE_MESSAGE.replace("{{ message_contact }}", contact);
        runCommand(requestWriteCommand);
        
        String returnValue = waitForOutput();
        
        if(returnValue.contains(">")) {
            String messageToWrite = (CMD_DO_WRITE_AFTER_REQUEST.replace("{{ message }}", message)).concat(String.valueOf(CTRL_Z));
            runCommand(messageToWrite);
            
            returnValue = waitForOutput();
            
            if(returnValue.contains("ERROR"))
            {
                return false;
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Save a number of messages to the SIM Card
     * 
     * @param contact
     * @param messages
     * @param clearAll
     * @return 
     */
    
    public boolean saveMessages(String contact, ArrayList<String> messages, boolean clearAll)
    {
        String returnValue = "";
        
        if(clearAll) {
            runCommand(CMD_DELETE_ALL_MESSAGES);
            returnValue = waitForOutput();
            
            if(returnValue == null || returnValue.contains("ERROR")) {
                return false;
            }
        }
        
        int numItems = messages.size();
        
        for(int itemLoop = 0; itemLoop < numItems; itemLoop ++)
        {
            // Rule out empty messages
            if(!messages.get(itemLoop).trim().equals(""))
            {
                // Save message
                if(!saveMessage(contact, messages.get(itemLoop)))
                {
                    return false;
                }
            } 
        }
        
        return true;
    }
    
    public boolean clearAllMessages()
    {
        String response = runCommand(CMD_DELETE_ALL_MESSAGES);
        
        if(response.trim().equals("OK"))
            return true;
        
        return false;
    }

    /**
     * getSerialPortList()
     * 
     * @return List of Serial Port Names
     */
    
    public ArrayList<String> getSerialPortList() {
        return serialPortList;
    }

    /**
     * Checks to see if the serial port is connected
     * 
     * @return 
     */
    
    public boolean isConnected() {
        return connected;
    }
    
    /**
     * Gets the port name
     * 
     * @return Port name
     */
    
    public String getPortName() {
        return serialPort.getName();
    }
}
