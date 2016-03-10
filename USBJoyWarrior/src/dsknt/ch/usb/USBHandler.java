package dsknt.ch.usb;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;



public class USBHandler /*implements Runnable,*/ 
{
	private int x, y, z;
	private double x_d, y_d, z_d;

	private final short VENDOR_ID = 0x07C0;
	private final short PRODUCT_ID = 0x1116;
	private final byte INTERFACE = 1;

	private final int MAX_CONTROL_OUT_TRANSFER_SIZE = 8;

	private static byte CONTROL_REQUEST_TYPE_IN = LibUsb.ENDPOINT_IN | LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE;
	private static byte CONTROL_REQUEST_TYPE_OUT = LibUsb.ENDPOINT_OUT | LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE;

	private final byte HID_GET_REPORT = 0x01;
	private final byte HID_SET_REPORT = 0x09;
	private final short HID_REPORT_TYPE_INPUT = 0x01;
	private final short HID_REPORT_TYPE_OUTPUT = 0x02;

	private final int TIMEOUT_MS = 0;
	public static final long READ_UPDATE_DELAY_MS = 5L;
	
	private final double SENSITIVITY = 8192;
	private final double G = 9.80665;
	
	private DeviceHandle handle;
	private int result = 0;

	public USBHandler()
	{
		result = LibUsb.init(null);
		if(result != LibUsb.SUCCESS)
		{
			throw new LibUsbException("Unable to initialize libusb", result);
		}
		handle = LibUsb.openDeviceWithVidPid(null, VENDOR_ID, PRODUCT_ID);
		if(handle == null)
		{
			System.err.println("Test device not found.");
			System.exit(1);
		}
		// Claim the ADB interface
		result = LibUsb.claimInterface(handle, INTERFACE);
		if(result != LibUsb.SUCCESS)
		{
			throw new LibUsbException("Unable to claim interface", result);
		}
	}

	public void doDataRead()
	{
		ByteBuffer buffer;
		byte[] data = new byte[MAX_CONTROL_OUT_TRANSFER_SIZE];
		int i, bytes_sent, bytes_received;

		for(i = 0; i < MAX_CONTROL_OUT_TRANSFER_SIZE; i++)
		{
			data[i] = 0;
		}
		data[0] = (byte)0x87; 	// 0x82 to read one byte 0x83 for two bytes
								// etc.
								// until 0x87;
		data[1] = (byte)(0x02 | 0x80);
		buffer = ByteBuffer.allocateDirect(MAX_CONTROL_OUT_TRANSFER_SIZE);
		bytes_sent = LibUsb.controlTransfer(handle, CONTROL_REQUEST_TYPE_OUT, HID_SET_REPORT, 
													(short)((HID_REPORT_TYPE_OUTPUT << 8) | 0x00), 
													(short)INTERFACE, buffer.put(data), 
													TIMEOUT_MS);
		if(bytes_sent >= 0)
		{
			LibUsb.clearHalt(handle, (byte)130);
			bytes_received = LibUsb.controlTransfer(handle, CONTROL_REQUEST_TYPE_IN, HID_GET_REPORT, 
															(short)((HID_REPORT_TYPE_INPUT << 8) | 0x00), 
															(short)INTERFACE, buffer, 
															TIMEOUT_MS);
		}
		for(int j = 0; j < buffer.capacity(); j++)
		{
			//System.out.print(Integer.toHexString(buffer.get(j))+"\t");
			System.out.print(buffer.get(j)+"\t");
		}
		System.out.println();
		x = ((buffer.get(3) << 8) | buffer.get(2)) >> 2;
		x_d = ((double)x/SENSITIVITY) *G;
		y = (((buffer.get(5) << 8) | buffer.get(4)) >> 2);
		y_d = ((double)y/SENSITIVITY) *G;
		z = ((buffer.get(7) << 8) | buffer.get(6)) >> 2;
		z_d = ((double)z/SENSITIVITY) *G;
		
		//System.out.println((float)x / 8192f + ", " + (float)y / 8192f + ", " + (float)z / 8192f);
		try
		{
			Thread.sleep(READ_UPDATE_DELAY_MS);
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
			result = LibUsb.releaseInterface(handle, INTERFACE);
			if(result != LibUsb.SUCCESS)
			{
				throw new LibUsbException("Unable to release interface", result);
			}
			LibUsb.close(handle);
			LibUsb.exit(null);
		}
	}

	public double getX()
	{
		return x_d;
	}

	public double getY()
	{
		return y_d;
	}

	public double getZ()
	{
		return z_d;
	}
}
