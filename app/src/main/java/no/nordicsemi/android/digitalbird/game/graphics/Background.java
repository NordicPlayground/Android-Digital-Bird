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
import android.util.TypedValue;

import no.nordicsemi.android.digitalbird.R;

public class Background implements GameObject {
	private final static float GRASS_SPEED = -1; // negative as pipes move to the left

	private final Paint mBackgroundPaint;
	private final Paint mCloudsPaint;
	private final Paint mCityPaint;
	private final Paint mGrassPaint;
	private final Paint mGroundPaint;
	private final int mCloudsHeight;
	private final int mCityWidth;
	private final int mCityHeight;
	private final int mGrassWidth;
	private final int mGrassHeight;
	private final int mGrassMargin;
	private int mCloudsPositionY;
	private Matrix mGrassMatrix;

	private int mWidth;
	private int mHeight;
	private float mTotalOffset;

	public Background(Resources resources) {
		Bitmap clouds = BitmapFactory.decodeResource(resources, R.drawable.background_clouds);
		BitmapShader cloudsShader = new BitmapShader(clouds, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		mCloudsHeight = clouds.getHeight();
		mCloudsPaint = new Paint();
		mCloudsPaint.setShader(cloudsShader);

		Bitmap city = BitmapFactory.decodeResource(resources, R.drawable.background_city);
		BitmapShader cityShader = new BitmapShader(city, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		mCityWidth = city.getWidth();
		mCityHeight = city.getHeight();
		mCityPaint = new Paint();
		mCityPaint.setShader(cityShader);

		Bitmap grass = BitmapFactory.decodeResource(resources, R.drawable.background_grass);
		BitmapShader grassShader = new BitmapShader(grass, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		mGrassWidth = grass.getWidth();
		mGrassHeight = grass.getHeight();
		mGrassMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, resources.getDisplayMetrics());
		mGrassPaint = new Paint();
		mGrassPaint.setShader(grassShader);

		mBackgroundPaint = new Paint();
		mBackgroundPaint.setColor(resources.getColor(R.color.background));
		mGroundPaint = new Paint();
		mGroundPaint.setColor(resources.getColor(R.color.ground));
	}

	@Override
	public void setScreenDimensions(int width, int height) {
		// Save the new screen dimensions.
		mWidth = width;
		mHeight = height;
		mCloudsPositionY = height * 3/4;

		// Create a local matrix for the paint that will draw clouds.
		Matrix matrix = new Matrix();
		matrix.postTranslate(0, mCloudsPositionY);
		mCloudsPaint.getShader().setLocalMatrix(matrix);

		// Create a local matrix for the paint that will draw the city.
		matrix = new Matrix();
		matrix.postTranslate( - mCityWidth + width, mCloudsPositionY + mCloudsHeight); // we want the background to be right adjusted
		mCityPaint.getShader().setLocalMatrix(matrix);

		// Create a local matrix for the paint that will draw the moving grass.
		matrix = mGrassMatrix = new Matrix();
		matrix.postTranslate(0, mCloudsPositionY + mCloudsHeight + mCityHeight);
		mGrassPaint.getShader().setLocalMatrix(matrix);
	}

	public int getGroundPositionY() {
		return mCloudsPositionY + mCloudsHeight + mCityHeight - mGrassMargin;
	}

	@Override
	public void move(float scaledDeltaTime) {
		float translation = scaledDeltaTime * GRASS_SPEED;

		// Below we are moving the grass matrix by a translation. As the translation is always a number around -8 eventually the transition grows to a Float.MAX_VALUE
		// and animation stops, therefore from time to time we have to reset the matrix. If the current translation would translate the grass matrix an integer number
		// of grass widths, we can simply reset the translation by setting the translation to a value equals minus total translation. Then, the translation from a big negative number
		// will be set to something around 0. This does not happen every time but happens sometimes. Statistically it should happen before the float overflow.
		mTotalOffset += translation;
		if (mTotalOffset % mGrassWidth == 0) {
			translation = -mTotalOffset;
			mTotalOffset = 0;
		}

		// Move the grass according to the translation
		mGrassMatrix.postTranslate(translation, 0);
		mGrassPaint.getShader().setLocalMatrix(mGrassMatrix);
	}

	@Override
	public void draw(final Canvas canvas) {
		int width = mWidth;
		int cloudsPositionY = mCloudsPositionY;
		int cloudsHeight = mCloudsHeight;
		int cityHeight = mCityHeight;
		int grassHeight = mGrassHeight;

		// Draw background
		canvas.drawPaint(mBackgroundPaint);
		canvas.drawRect(0, cloudsPositionY, width, cloudsPositionY + cloudsHeight, mCloudsPaint);
		canvas.drawRect(0, cloudsPositionY + cloudsHeight, width, cloudsPositionY + cloudsHeight + cityHeight, mCityPaint);
		canvas.drawRect(0, cloudsPositionY + cloudsHeight + cityHeight, width, cloudsPositionY + cloudsHeight + cityHeight + grassHeight, mGrassPaint);
		canvas.drawRect(0, cloudsPositionY + cloudsHeight + cityHeight + grassHeight, width, mHeight, mGroundPaint);
	}
}
