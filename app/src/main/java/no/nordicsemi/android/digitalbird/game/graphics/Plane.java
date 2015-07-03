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

public class Plane implements GameObject {
	private final static float WIND_SPEED = 80; // The higher number, the slower the sprites are changing.
	private final static float PLANE_SPEED = 6; // The higher number, the slower the plane flies.

	private final Paint mPlanePaint;
	private final Paint mPlaneReturnsPaint;
	private final int mPlaneWidth;
	private final int mPlaneHeight;
	private Matrix mPlaneMatrix;

	/** Screen width. */
	private int mWidth;
	/** The available screen height. */
	private int mHeight;
	/** The X position of the top left corner of the plane. */
	private float mPositionX;
	/** The Y position of the top left corner of the plane. */
	private float mPositionY;
	/** A temporary value used to calculate the plane sprite index. */
	private float mTotalDeltaTime;
	/** Flag set to true if the plane goes from right to left. Initially it's set to true. */
	private boolean mPlaneFlyingFromRight;

	public Plane(final Resources resources) {
		Bitmap plane = BitmapFactory.decodeResource(resources, R.drawable.plane_sprite);
		BitmapShader planeShader = new BitmapShader(plane, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		mPlaneWidth = plane.getWidth() / 3; // There are 3 plane images in the image.
		mPlaneHeight = plane.getHeight();
		mPlanePaint = new Paint();
		mPlanePaint.setShader(planeShader);

		Bitmap planeReturns = BitmapFactory.decodeResource(resources, R.drawable.plane_returns_sprite);
		BitmapShader planeReturnsShader = new BitmapShader(planeReturns, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		mPlaneReturnsPaint = new Paint();
		mPlaneReturnsPaint.setShader(planeReturnsShader);
	}

	@Override
	public void setScreenDimensions(final int width, final int height) {
		mWidth = width;
		mHeight = height;

		// Initial plane position and direction
		mPlaneFlyingFromRight = true;
		mPositionX = width * 2;
		mPositionY = height / 4;

		// Create a local matrix for the paint that will draw the plane.
		final Matrix matrix = mPlaneMatrix = new Matrix();
		matrix.postTranslate(mPositionX, mPositionY);
		mPlanePaint.getShader().setLocalMatrix(matrix);
		mPlaneReturnsPaint.getShader().setLocalMatrix(matrix);
	}

	@Override
	public void move(float scaledDeltaTime) {
		calculateSprite(scaledDeltaTime);

		// When plane reaches the end of the city it turns back.
		if (mPlaneFlyingFromRight && mPositionX + mPlaneWidth < - mWidth / 2) {
			mPlaneFlyingFromRight = false;
			mPositionY = (float) (mHeight / 10 + Math.random() * mHeight / 2.0f);
		} else if (!mPlaneFlyingFromRight && mPositionX > mWidth * 4 / 3) {
			mPlaneFlyingFromRight = true;
			mPositionY = (float) (mHeight / 10 + Math.random() * mHeight / 2.0f);
		}
	}

	private void calculateSprite(final float scaledDeltaTime) {
		// Accumulate the total scaledDeltaTime. The total scaledDeltaTime is used to calculate the sprite index.
		mTotalDeltaTime += scaledDeltaTime;

		if (mPlaneFlyingFromRight) {
			// Calculate the Y position based on scaledDeltaTime
			mPositionX -= scaledDeltaTime * 1.1f / PLANE_SPEED;
		} else {
			mPositionX += scaledDeltaTime * 0.95f / PLANE_SPEED; // on the way back the plane is further away, so it's flying slower
		}

		float spriteOffset = 0.0f;
		if (mTotalDeltaTime > WIND_SPEED)
			spriteOffset += mPlaneWidth;
		if (mTotalDeltaTime > WIND_SPEED * 2)
			spriteOffset += mPlaneWidth;
		if (mTotalDeltaTime > WIND_SPEED * 3) {
			mTotalDeltaTime = 0;
			spriteOffset = 0;
		}

		mPlaneMatrix.reset();
		mPlaneMatrix.postTranslate(mPositionX - spriteOffset, mPositionY);

		if (mPlaneFlyingFromRight)
			mPlanePaint.getShader().setLocalMatrix(mPlaneMatrix);
		else
			mPlaneReturnsPaint.getShader().setLocalMatrix(mPlaneMatrix);
	}

	@Override
	public void draw(final Canvas canvas) {
		canvas.drawRect(mPositionX, mPositionY, mPositionX + mPlaneWidth, mPositionY + mPlaneHeight, mPlaneFlyingFromRight ? mPlanePaint : mPlaneReturnsPaint);
	}
}
