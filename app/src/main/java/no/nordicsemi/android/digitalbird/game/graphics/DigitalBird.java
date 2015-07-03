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
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.TypedValue;

import no.nordicsemi.android.digitalbird.R;

public class DigitalBird implements GameObject {
	private final static float WINGS_SPEED = 50; // The higher number, the slower the wings are moving.

	private final Paint mBirdPaint;
	private final int mBirdBorderWidth;
	private final int mBirdWidth;
	private final int mBirdHeight;
	private Matrix mBirdMatrix;
	/** A temporary rectangle to calculate collisions. */
	private final Rect mTestRect = new Rect();

	/** Screen width. */
	private int mWidth;
	/** The available screen height. */
	private int mHeight;
	/** The X position of the top left corner of the bird. */
	private int mPositionX;
	/** The Y position of the top left corner of the bird. */
	private int mPositionY;
	/** Bird's vertical speed. The speed is set to a constant positive value when user press the button and decremented every frame. */
	private float mVerticalSpeed;
	/** A flag indicating whether the bird is under the user control. If false the bird will not fall down. This is set to true when game is started. */
	private boolean mUnderUserControl;
	/** A temporary value used to calculate the bird sprite index. */
	private float mTotalDeltaTime;

	public DigitalBird(final Resources resources) {
		Bitmap bird = BitmapFactory.decodeResource(resources, R.drawable.bird_sprite);
		BitmapShader birdShader = new BitmapShader(bird, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
		mBirdWidth = bird.getWidth() / 3; // There are 3 bird images in the image.
		mBirdHeight = bird.getHeight();
		mBirdPaint = new Paint();
		mBirdPaint.setShader(birdShader);

		mVerticalSpeed = 0.0f;
		mBirdBorderWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 7, resources.getDisplayMetrics());
	}

	@Override
	public void setScreenDimensions(final int width, final int height) {
		mWidth = width;
		mHeight = height;
		mPositionX = width / 3;
		mPositionY = height / 2;

		// Create a local matrix for the paint that will draw the bird.
		final Matrix matrix = mBirdMatrix = new Matrix();
		matrix.postTranslate(mPositionX, mPositionY);
		mBirdPaint.getShader().setLocalMatrix(matrix);
	}

	public void fly() {
		mUnderUserControl = true;
		mVerticalSpeed = 2.4f;
	}

	public boolean isAlive() {
		return mPositionY < mHeight - mBirdHeight;
	}

	public void reset() {
		mUnderUserControl = false;
		mVerticalSpeed = 0.0f;
		mPositionX = mWidth / 3;
		mPositionY = mHeight / 2;

		// Create a local matrix for the paint that will draw the bird.
		mBirdMatrix.postTranslate(mPositionX, mPositionY);
		mBirdPaint.getShader().setLocalMatrix(mBirdMatrix);
	}

	public boolean intersect(final Rect rect) {
		mTestRect.set(mPositionX + mBirdBorderWidth, mPositionY + mBirdBorderWidth, mPositionX + mBirdWidth - mBirdBorderWidth, mPositionY + mBirdHeight - mBirdBorderWidth);
		final boolean collision = rect.intersect(mTestRect);
		if (collision && mVerticalSpeed > -1.0f)
			mVerticalSpeed = -1.0f;
		return collision;
	}

	/* package */int getPositionX() {
		return mPositionX + mBirdBorderWidth;
	}

	@Override
	public void move(float scaledDeltaTime) {
		calculateSprite(scaledDeltaTime);

		if (mUnderUserControl)
			mVerticalSpeed -= 0.12f;
	}

	private void calculateSprite(final float scaledDeltaTime) {
		// Calculate the bird's angle based on it's vertical velocity
		final float angle = mUnderUserControl ? (mVerticalSpeed >= -0.4 ? -30.0f : Math.min(90.0f, -mVerticalSpeed * 30.0f - 32.0f)) : 0.0f;

		// Accumulate the total scaledDeltaTime. The total scaledDeltaTime is used to calculate the sprite index.
		mTotalDeltaTime += scaledDeltaTime;

		// Calculate the Y position based on scaledDeltaTime and vertical velocity
		mPositionY -= mVerticalSpeed * scaledDeltaTime;

		float spriteOffset = 0.0f;
		if (mTotalDeltaTime > WINGS_SPEED)
			spriteOffset += mBirdWidth;
		if (mTotalDeltaTime > WINGS_SPEED * 2)
			spriteOffset += mBirdWidth;
		if (mTotalDeltaTime > WINGS_SPEED * 3) {
			mTotalDeltaTime = 0;
			spriteOffset = 0;
		}

		mBirdMatrix.reset();
		mBirdMatrix.postTranslate(-spriteOffset, 0);
		mBirdMatrix.postRotate(angle, mBirdWidth / 2, mBirdHeight / 2);
		mBirdMatrix.postTranslate(mPositionX, mPositionY);
		mBirdPaint.getShader().setLocalMatrix(mBirdMatrix);
	}

	@Override
	public void draw(final Canvas canvas) {
		canvas.drawRect(mPositionX, mPositionY, mPositionX + mBirdWidth, mPositionY + mBirdHeight, mBirdPaint);
	}
}
