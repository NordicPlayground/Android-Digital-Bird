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

package no.nordicsemi.android.digitalbird.game.graphics;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;

import no.nordicsemi.android.digitalbird.R;

public class Fireworks implements GameObject {
	private final static float FRAME_SPEED = 120; // The higher number, the slower the sprites are changing.

	private final Paint mFireworksPaint;
	private final int mFireworksWidth;
	private final int mFireworksHeight;
	private Matrix mFireworksMatrix;

	/** The X position of the top left corner of the plane. */
	private float mPositionX;
	/** The Y position of the top left corner of the plane. */
	private float mPositionY;
	/** A temporary value used to calculate the plane sprite index. */
	private float mTotalDeltaTime;

	public Fireworks(final Resources resources) {
		Bitmap fireworks = BitmapFactory.decodeResource(resources, R.drawable.fireworks_sprite);
		BitmapShader planeShader = new BitmapShader(fireworks, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		mFireworksWidth = fireworks.getWidth() / 5; // There are 5 fireworks images in the image.
		mFireworksHeight = fireworks.getHeight();
		mFireworksPaint = new Paint();
		mFireworksPaint.setShader(planeShader);
	}

	@Override
	public void setScreenDimensions(final int width, final int height) {
		// Fireworks position
		mPositionX = width - mFireworksWidth;
		mPositionY = height  - mFireworksHeight;

		// Create a local matrix for the paint that will draw the plane.
		final Matrix matrix = mFireworksMatrix = new Matrix();
		matrix.postTranslate(mPositionX, mPositionY);
		mFireworksPaint.getShader().setLocalMatrix(matrix);
	}

	public void reset() {
		mTotalDeltaTime = 0;
	}

	@Override
	public void move(float scaledDeltaTime) {
		calculateSprite(scaledDeltaTime);
	}

	private void calculateSprite(final float scaledDeltaTime) {
		// Accumulate the total scaledDeltaTime. The total scaledDeltaTime is used to calculate the sprite index.
		mTotalDeltaTime += scaledDeltaTime;

		float spriteOffset = 0.0f;
		if (mTotalDeltaTime > FRAME_SPEED)
			spriteOffset += mFireworksWidth;
		if (mTotalDeltaTime > FRAME_SPEED * 2)
			spriteOffset += mFireworksWidth;
		if (mTotalDeltaTime > FRAME_SPEED * 3)
			spriteOffset += mFireworksWidth;
		if (mTotalDeltaTime > FRAME_SPEED * 4)
			spriteOffset += mFireworksWidth;
		if (mTotalDeltaTime > FRAME_SPEED * 5) {
			// The first frame is displayed only once per Game Over.
			mTotalDeltaTime = FRAME_SPEED;
			spriteOffset = mFireworksWidth;
		}

		mFireworksMatrix.reset();
		mFireworksMatrix.postTranslate(mPositionX - spriteOffset, mPositionY);

		mFireworksPaint.getShader().setLocalMatrix(mFireworksMatrix);
	}

	@Override
	public void draw(final Canvas canvas) {
		canvas.drawRect(mPositionX, mPositionY, mPositionX + mFireworksWidth, mPositionY + mFireworksHeight, mFireworksPaint);
	}
}
