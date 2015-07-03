/*************************************************************************************************************************************************
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************************************************************************************/

package no.nordicsemi.android.digitalbird.ble.scanner;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

/**
 * The Bluetooth Smart scanner interface. The scanner scans for a device that advertises with the given service UUID and reports the found device with
 * a callback to the sender. There are two implementations of the scanner. For Android 4.3 and 4.4 the old API is used and the scan result is parsed manually.
 */
public interface BluetoothSmartScanner {
	/** Scanning interval used to restart the scanning process in order to make the 'touch to pair' feature working on some devices (Nexus 4, Nexus 7 (2013)). */
	public final static long SCANNING_INTERVAL = 2000; // [ms]

	/** The minimum required value of the RSSI of the signal from the device to report. The higher (less negative) value, the closer the phone mush be to the board. */
	public static final int REQUIRED_RSSI = -35;

	/** The service UUID required in the advertising packet. */
	public static final UUID ADVERTISING_SERVICE_UUID = UUID.fromString("88400001-e95a-844e-c53f-fbec32ed5e54");

	/**
	 * The type of the EIR field in the advertising packet.
	 * See: <a href="https://www.bluetooth.org/en-us/specification/assigned-numbers/generic-access-profile">https://www.bluetooth.org/en-us/specification/assigned-numbers/generic-access-profile</a> for more info.
	 */
	public static final int GAP_COMPLETE_LIST_OR_128_BIT_SERVICE_CLASS_UUID = 0x07;

	/**
	 * The scanner callback.
	 */
	public interface Callback {
		/**
		 * Method called when the scanned finds device with required service UUID in the advertisement packet.
		 * @param device the device that has been found
		 */
		public void onDeviceFound(final BluetoothDevice device);
	}

	public void scan(final Callback callback);

	public void stop();
}
