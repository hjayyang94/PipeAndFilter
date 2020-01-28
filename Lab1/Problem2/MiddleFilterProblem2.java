/******************************************************************************************************************
* File:MiddleFilter.java
* Project: Lab 1
* Copyright:
*   Copyright (c) 2020 University of California, Irvine
*   Copyright (c) 2003 Carnegie Mellon University
* Versions:
*   1.1 January 2020 - Revision for SWE 264P: Distributed Software Architecture, Winter 2020, UC Irvine.
*   1.0 November 2008 - Sample Pipe and Filter code (ajl).
*
* Description:
* This class serves as an example for how to use the FilterRemplate to create a standard filter. This particular
* example is a simple "pass-through" filter that reads data from the filter's input port and writes data out the
* filter's output port.
* Parameters: None
* Internal Methods: None
******************************************************************************************************************/
import java.lang.Math;
import java.io.*;
import java.nio.*;
import java.util.*;

public class MiddleFilterProblem2 extends FilterFramework
{
	public void run()
    {
		String filePath = "WildPoints.csv";
		FileWriter csvWriter = null;
		try{
			csvWriter = new FileWriter(filePath);
		} catch( FileNotFoundException e){
			e.printStackTrace();
		} catch ( IOException e){
			e.printStackTrace();
		}



		int bytesread = 0;					// Number of bytes read from the input file.
		int byteswritten = 0;				// Number of bytes written to the stream.
		byte databyte = 0;					// The byte of data read from the file

		// Next we write a message to the terminal to let the world know we are alive...
		System.out.print( "\n" + this.getName() + "::Middle Reading ");

		int MeasurementLength = 8;		// This is the length of all measurements (including time) in bytes
		int IdLength = 4;				// This is the length of IDs in the byte stream
		long measurement;				// This is the word used to store all measurements - conversions are illustrated.
		int id;							// This is the measurement id
		int i;							// This is a loop counter
		int counter = 0;				// This is a counter to make sure we read 2 data points before starting averages.

		double prev1 = 0;				// This is a cached double to record the
		double prev2 = 0;				// last 2 data points to calculate average
		double current;					// This is the current altitude data;

		int byteBufferCapactiy = 8;		// This is the capacity our byte buffer will have
					//


		try {
			csvWriter.append("Original Value,");
			csvWriter.append("New Value");
			csvWriter.append("\n");        // We are done with our headers, and now ready to input our data
		}
		catch(Exception e){
			e.printStackTrace();
		}


		while (true)
		{
			try
			{
				/***************************************************************************
				 // We know that the first data coming to this filter is going to be an ID and
				 // that it is IdLength long. So we first get the ID bytes.
				 ****************************************************************************/
				id = 0;
				for (i=0; i<IdLength; i++ )
				{
					databyte = ReadFilterInputPort();	// This is where we read the byte from the stream...
					id = id | (databyte & 0xFF);		// We append the byte on to ID...
					if (i != IdLength-1)				// If this is not the last byte, then slide the
					{									// previously appended byte to the left by one byte
						id = id << 8;					// to make room for the next byte we append to the ID
					}
					bytesread++;						// Increment the byte count

					WriteFilterOutputPort(databyte);	// Since we won't be changing the data of the ID's,
														// We will send the databytes as is.
					byteswritten++;
				}

				/****************************************************************************
				 // Here we read measurements. All measurement data is read as a stream of bytes
				 // and stored as a long value. This permits us to do bitwise manipulation that
				 // is neccesary to convert the byte stream into data words. Note that bitwise
				 // manipulation is not permitted on any kind of floating point types in Java.
				 // If the id = 0 then this is a time value and is therefore a long value - no
				 // problem. However, if the id is something other than 0, then the bits in the
				 // long value is really of type double and we need to convert the value using
				 // Double.longBitsToDouble(long val) to do the conversion which is illustrated below.
				 *****************************************************************************/
				measurement = 0;


				for (i=0; i<MeasurementLength; i++ )
				{
					databyte = ReadFilterInputPort();
					measurement = measurement | (databyte & 0xFF);	// We append the byte on to measurement...
					if (i != MeasurementLength-1)					// If this is not the last byte, then slide the
					{												// previously appended byte to the left by one byte
						measurement = measurement << 8;				// to make room for the next byte we append to the
																	// measurement

					}
					bytesread++;									// Increment the byte count
				}




				/****************************************************************************
				 // Here we pick up a measurement (ID = 4 in this case), but you can pick up
				 // any measurement you want to. All measurements in the stream are
				 // captured by this class. Note that all data measurements are double types
				 // This illustrates how to convert the bits read from the stream into a double
				 // type. Its pretty simple using Double.longBitsToDouble(long value). So here
				 // we print the time stamp and the data associated with the ID we are interested in.
				 ****************************************************************************/
				if ( id != 2 )
				{
					current = Double.longBitsToDouble(measurement);
					byte[] byteArr = doubleToByteArray(current);
					sendByteArray(byteArr);
					byteswritten += 8;


				}
				else if ( id == 2 ) 										// If the data is for altitude
				{
					counter++;
					current = Double.longBitsToDouble(measurement); 		// Converting bytes to double
					if ( Math.abs(current - prev1) > 100 && counter > 2 )	// If the current value is a "wild jump" and not the first two data points.
					{
						double newValue = (prev2 + prev1) / (2.0);			// We calculate the average of the last two data points to replace the "wildjump" value
						double negValue = newValue * -1;					// A negative value is used to indicate that the value was changed so Sink Filter can recognize

						byte[] byteArr = doubleToByteArray( negValue );
						sendByteArray( byteArr );							// We pass the new value on.
						byteswritten += 8;

						try{
							csvWriter.append(current+",");					// Record original and new values in WildJumps.csv
							csvWriter.append(newValue+"\n");
							current = newValue;
						} catch (Exception e){
							e.printStackTrace();
						}
					}

					else{
						byte[] byteArr = doubleToByteArray(current);
						sendByteArray(byteArr);
						byteswritten += 8;
					}
					prev2 = prev1;
					prev1 = current;
				}
			}
			/*******************************************************************************
			 *	The EndOfStreamExeception below is thrown when you reach end of the input
			 *	stream. At this point, the filter ports are closed and a message is
			 *	written letting the user know what is going on.
			 ********************************************************************************/
			catch (EndOfStreamException e)
			{
				ClosePorts();
				System.out.print( "\n" + this.getName() + "::Middle Exiting; bytes read: " + bytesread + " bytes written: " + byteswritten );
				try{
					csvWriter.close();
				}
				catch(IOException er){

					er.printStackTrace();
				}

				break;
			}

		} // while
   }

   private static byte[] doubleToByteArray(double value){
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putDouble(value);
		return bytes;
   }

   private void sendByteArray(byte[] arr )
   {
		for ( int i = 0; i < arr.length; i++ ){
			WriteFilterOutputPort(arr[i]);

		}
   }

   private static void sendLongByte( long value ){

   }

}