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

public class Pipe implements GameObject {
	private final static float PIPE_SPEED = -1; // negative as pipes move to the left

	private final Paint mPipePaint;
	private final Paint mTopValvePaint;
	private final Paint mBottomValvePaint;
	private final int mPipeWidth;
	private final int mValveWidth;
	private final int mValveHeight;
	private final int mSpanHeight;
	private Matrix mPipeMatrix;
	private Matrix mTopValveMatrix;
	private Matrix mBottomValveMatrix;
	/** A temporary rectangle to calculate collisions. */
	private final Rect mTestRect = new Rect();

	/** Screen width. */
	private int mWidth;
	/** Height of the available space. */
	private int mHeight;
	/** The minimum value of the span position Y. The span may not start too high (end of screen). */
	private int mMinSpanPositionY;
	/** The maximum value of the span position Y. The span may not end too log (grass). */
	private int mMaxSpanPositionY;
	/** The Y position of the top of the span. */
	private int mSpanPositionY;
	/** The current X position of the pipe. The pipe moves from right ot left, so the X position changes from mWidth to -mValveWidth. */
	private float mPositionX;
	/** Flag indicating whether the pipe has been passed by the bird. */
	private boolean mPassed;

	public Pipe(final Resources resources) {
		Bitmap pipe = BitmapFactory.decodeResource(resources, R.drawable.pipe);
		BitmapShader pipeShader = new BitmapShader(pipe, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		mPipeWidth = pipe.getWidth();
		mPipePaint = new Paint();
		mPipePaint.setShader(pipeShader);

		Bitmap topValve = BitmapFactory.decodeResource(resources, R.drawable.pipe_top_valve);
		BitmapShader topValveShader = new BitmapShader(topValve, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		mValveWidth = topValve.getWidth();
		mValveHeight = topValve.getHeight();
		mTopValvePaint = new Paint();
		mTopValvePaint.setShader(topValveShader);

		Bitmap bottomValve = BitmapFactory.decodeResource(resources, R.drawable.pipe_bottom_valve);
		BitmapShader bottomValveShader = new BitmapShader(bottomValve, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		mBottomValvePaint = new Paint();
		mBottomValvePaint.setShader(bottomValveShader);

		mSpanHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, resources.getDisplayMetrics());
		mPassed = false;
	}

	@Override
	public void setScreenDimensions(final int width, final int height) {
		// Save the new screen dimensions.
		mWidth = width;
		mHeight = height;

		mMinSpanPositionY = mHeight / 8;
		mMaxSpanPositionY = mHeight *7/8;
	}

	/**
	 * Sets the X offset to out of screen and randomize the span position.
	 */
	public Pipe reset() {
		final float initPosition = mPositionX = mWidth;
		final float pipeOffset = (mValveWidth - mPipeWidth) / 2;
		final int spanPositionY = mSpanPositionY = mMinSpanPositionY + (int) (Math.random() * (mMaxSpanPositionY - mMinSpanPositionY - mSpanHeight));

		// Create a local matrix for the paint that will draw the pipes.
		Matrix matrix = mPipeMatrix = new Matrix();
		matrix.postTranslate(initPosition + pipeOffset, 0);
		mPipePaint.getShader().setLocalMatrix(matrix);

		matrix = mTopValveMatrix = new Matrix();
		matrix.postTranslate(initPosition, spanPositionY - mValveHeight); // -mValveHeight is unnecessary
		mTopValvePaint.getShader().setLocalMatrix(matrix);

		matrix = mBottomValveMatrix = new Matrix();
		matrix.postTranslate(initPosition, spanPositionY + mSpanHeight);
		mBottomValvePaint.getShader().setLocalMatrix(matrix);

		mPassed = false;
		return this;
	}

	/**
	 * Checks the collision between the pipe and the Digital Bird.
	 * @param bird the Digital Bird object
	 * @return true if the bird hit the pipe, false otherwise
	 */
	public boolean checkCollision(final DigitalBird bird) {
		final int positionX = (int) mPositionX;
		mTestRect.set(positionX, -Integer.MAX_VALUE, positionX + mValveWidth, mSpanPositionY);
		if (bird.intersect(mTestRect))
			return true;

		mTestRect.set(positionX, mSpanPositionY + mSpanHeight, positionX + mValveWidth, mHeight);
		return bird.intersect(mTestRect);
	}

	/**
	 * Returns true if the pips is no longer visible and may be released for future use.
	 * @return true if pipe is ready to be released
	 */
	public boolean isPipeReadyToRelease() {
		return mPositionX + mValveWidth < 0;
	}

	public boolean isPipeScored(final DigitalBird bird) {
		if (!mPassed && bird.getPositionX() > mPositionX + mPipeWidth) {
			mPassed = true;
			return true;
		}

		return false;
	}

	@Override
	public void move(final float scaledDeltaTime) {
		final float translation = scaledDeltaTime * PIPE_SPEED;
		mPipeMatrix.postTranslate(translation, 0);
		mTopValveMatrix.postTranslate(translation, 0);
		mBottomValveMatrix.postTranslate(translation, 0);
		mPipePaint.getShader().setLocalMatrix(mPipeMatrix);
		mTopValvePaint.getShader().setLocalMatrix(mTopValveMatrix);
		mBottomValvePaint.getShader().setLocalMatrix(mBottomValveMatrix);
		mPositionX += translation;
	}

	@Override
	public void draw(final Canvas canvas) {
		int pipeWidth = mPipeWidth;
		int valveWidth = mValveWidth;
		int valveHeight = mValveHeight;
		float valvePositionX = mPositionX;
		float pipePositionX = valvePositionX + (valveWidth - pipeWidth) / 2;
		float spanPositionY = mSpanPositionY;
		float spanHeight = mSpanHeight;

		canvas.drawRect(pipePositionX, 0, pipePositionX + pipeWidth, spanPositionY - valveHeight, mPipePaint);
		canvas.drawRect(valvePositionX, spanPositionY - valveHeight, valvePositionX + valveWidth, spanPositionY, mTopValvePaint);
		canvas.drawRect(valvePositionX, spanPositionY + spanHeight, valvePositionX + valveWidth, spanPositionY + spanHeight + valveHeight, mBottomValvePaint);
		canvas.drawRect(pipePositionX, spanPositionY + spanHeight + valveHeight, pipePositionX + pipeWidth, mHeight, mPipePaint);
	}
}
