package com.voidstar.glass.nerf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BlasterManager {
	private final Set<OnChangedListener> listeners;

	private BluetoothAdapter bluetoothAdapter;
	private BluetoothDevice arduino;
	private BluetoothSocket sppSocket;
	private BufferedInputStream sppRx;
	private BufferedOutputStream sppTx;

	private boolean isAlive = true;
	private int lastRead;
	private List<Integer> activeSentence = new ArrayList<Integer>();

	private int magSize;
	private int ammoLeft;
	private boolean isArmed;
	private boolean magIsInserted;

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.d("Nerf", "Discovered device " + device.getName());

				if (device.getName().equals("Nerf")) {
					bluetoothAdapter.cancelDiscovery();

					try {
						sppSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")); // Magic Number UUID for SPP
						sppSocket.connect();
						sppSocket.close();
						sppSocket = null;
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	};
	public interface OnChangedListener {
		void onStatsChanged(BlasterManager blasterManager);
	}


	public void addOnChangedListener(OnChangedListener listener) {
		listeners.add(listener);
	}


	public void removeOnChangedListener(OnChangedListener listener) {
		listeners.remove(listener);
	}


	public BlasterManager() {
		listeners = new LinkedHashSet<OnChangedListener>();
	}


	public void Pair(Context context) {
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		bluetoothAdapter.cancelDiscovery();
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		context.registerReceiver(mReceiver, filter);
		bluetoothAdapter.startDiscovery();
	}


	public void Connect() {
		class BlasterConnector implements Runnable {
			public void run() {
				bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
				for (BluetoothDevice device : pairedDevices) {
					Log.i("Nerf", "Found Paired Device: " + device.getName());
					if (device.getName().equals("Nerf")) {
						Log.i("Nerf", "That's our man");
						arduino = device;
					}
				}

				try {
					sppSocket = arduino.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")); // Magic Number UUID for SPP
				}
				catch (Exception e){
					Log.e("Nerf", "UUID is bad or couldn't connect. Sucks to be you");
					e.printStackTrace();
					return;
				}

				Log.i("Nerf", "Connecting to SPP");

				try {
					bluetoothAdapter.cancelDiscovery();
					sppSocket.connect();
				}
				catch (IOException e) {
					Log.e("Nerf", "Failed to connect - IOException");
					e.printStackTrace();
					return;
				}

				Log.i("Nerf", "Connected..?");

				try {
					sppRx = new BufferedInputStream(sppSocket.getInputStream());
					sppTx = new BufferedOutputStream(sppSocket.getOutputStream());
				}
				catch (IOException e) {
					Log.e("Nerf", "Failed to open Rx and/or Tx");
					e.printStackTrace();
					return;
				}

				generateBlasterCommunicator();
			}
		}

		new Thread(new BlasterConnector()).start();
	}

	private void generateBlasterCommunicator() {
		BlasterCommunicator communicator = new BlasterCommunicator();
		new Thread(communicator).start();
	}

	public void Disconnect() {
		try {
			isAlive = false;
			if (sppRx != null) sppRx.close();
			if (sppTx != null) sppTx.close();
			if (sppSocket != null) sppSocket.close();
		}
		catch (IOException e) {
			Log.e("Nerf", "Closing sockets failed");
		}

		Log.i("Nerf", "Closed sockets");
	}    	


	private void notifyStatsChanged() {
		for (OnChangedListener listener : listeners) listener.onStatsChanged(this);
	}

	public int getMagSize() {
		return magSize;
	}


	public int getAmmoLeft() {
		return ammoLeft;
	}


	public boolean getIsArmed() {
		return isArmed;
	}


	public boolean getMagInserted() {
		return magIsInserted;
	}


	public class BlasterCommunicator implements Runnable {
		public void run() {
			Log.i("Nerf", "Started new BlasterCommunicator.");

			//static long lastUpdate = 0; 

			println("~");

			while(isAlive) {
				try {
					if (!sppSocket.isConnected()) sppSocket.connect();
				}
				catch (IOException e) {
					Log.e("Nerf", "Error while connecting");
					Log.e("Nerf", e.getLocalizedMessage());
					e.printStackTrace();
				}

				lastRead = -1;

				try {
					//Log.d("Nerf", "Preparing to read");
					if (sppRx.available() > 0) {
						lastRead = sppRx.read();
						//Log.d("Nerf", "Read " + Integer.toString(lastRead));
					}
					//else Log.d("Nerf", "Nothing to read");
				}
				catch (IOException e) {
					Log.e("Nerf", "IOException while reading from RX buffer");
					Log.e("Nerf", e.getLocalizedMessage());
				}

				if (lastRead == '>') {
					if ( activeSentence.size() >= 5 &&
							activeSentence.get(0) == '<') {		
						int newAmmoLeft = activeSentence.get(1);
						int newMagSize = activeSentence.get(2);
						boolean newIsArmed = activeSentence.get(3) == 1;
						boolean newMagIsInserted = activeSentence.get(4) == 1;
						
						/* Important! Updating a RemoteView is intense. Do it only when something changes. */
						if (newAmmoLeft != ammoLeft || 
							newMagSize != magSize ||
							newIsArmed != isArmed ||
							newMagIsInserted != magIsInserted) {
							/*
							Log.d("Nerf", "Rx Ammo Left: " + Integer.toString(ammoLeft) +
									" Mag: " + Integer.toString(magSize) +
									" Armed: " + Boolean.toString(isArmed) +
									" Mag: " + Boolean.toString(magIsInserted));
							 */	

							ammoLeft = newAmmoLeft;
							magSize = newMagSize;
							isArmed = newIsArmed;
							magIsInserted = newMagIsInserted;
							
							notifyStatsChanged();
						}

						println("~");
					}

					activeSentence.clear();
				}
				else if (lastRead != -1) {
					activeSentence.add(lastRead);
				}

				/*
				if (SystemClock.elapsedRealtime() - lastUpdate > 100) {
					Log.d("Nerf", "Polling");
					println("~");
					lastUpdate = SystemClock.elapsedRealtime();
				}
				 */
			}
		}


		private void println(String command) {
			try {
				//Log.i("Nerf", "Printing command " + command);
				sppTx.write(command.getBytes());
				sppTx.write('\r');
				sppTx.flush();
			}
			catch (IOException e) { Log.e("Nerf", e.getLocalizedMessage()); }  
		}
	}

}
