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

package no.nordicsemi.android.digitalbird.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class DigitalBirdBleManager extends BleManager<DigitalBirdBleManagerCallbacks> {
	public static final UUID BUTTON_SERVICE_UUID        = UUID.fromString("88400001-e95a-844e-c53f-fbec32ed5e54");
	public static final UUID BUTTON_CHARACTERISTIC_UUID = UUID.fromString("88400002-e95a-844e-c53f-fbec32ed5e54");

	private BluetoothGattCharacteristic mButtonCharacteristic;

	private final BleManagerGattCallback mCallback = new BleManagerGattCallback() {
		@Override
		protected boolean isRequiredServiceSupported(final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(BUTTON_SERVICE_UUID);
			if (service != null) {
				mButtonCharacteristic = service.getCharacteristic(BUTTON_CHARACTERISTIC_UUID);
			}
			return mButtonCharacteristic != null;
		}

		@Override
		protected Queue<Request> initGatt(final BluetoothGatt gatt) {
			// As the initialization we need to enable notifications for the Button Characteristic
			final LinkedList<Request> requests = new LinkedList<>();
			requests.push(Request.newEnableNotificationsRequest(mButtonCharacteristic));
			return requests;
		}

		@Override
		protected void onDeviceDisconnected() {
			// Just clear references here.
			mButtonCharacteristic = null;
		}

		@Override
		protected void onCharacteristicNotified(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
			// There is only one characteristic that has notifications enabled.
			final int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
			if (value == 0x01)
				mCallbacks.onButtonPressed();
			// else
			//  button released
		}
	};

	public DigitalBirdBleManager(final Context context) {
		super(context);
	}

	@Override
	protected BleManagerGattCallback getGattCallback() {
		return mCallback;
	}
}
