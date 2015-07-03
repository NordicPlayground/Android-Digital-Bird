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

package no.nordicsemi.android.digitalbird.game;

import android.content.res.Resources;
import android.view.SurfaceView;

public class GameEngine implements GameLogic.GameStateListener {
	private static final String TAG = "GameEngine";

	/**
	 * The speed modifier. The bigger number, the faster the game will be.
	 * The speed modifier concerns all objects, including the bird.
	 */
	private static final float SPEED_MODIFIER = 0.125f;

	/**
	 * The interval between pipes, in milliseconds.
	 */
	private static final long PIPES_INTERVAL = 2400; // [ms]

	public interface GameListener {
		/**
		 * User has scored points.
		 * @param points total number of points scored since game was started
		 */
		public void onScore(final int points);

		/**
		 * Game over.
		 * @param points total number of points scored since game was started
		 * @return true if the record has been beaten
		 */
		public boolean onGameOver(final int points);
	}

	private DigitalBirdGameLogic mGameLogic;
	private GameListener mGameListener;

	public GameEngine(final SurfaceView surfaceView) {
		// Calculate the game speed. The game speed depends on the screen density.
		// On higher densities objects must travel more pixels to have the same speed in cm/s.
		// Modify the SPEED_MODIFIER to make the game faster or slower.
		final Resources resources = surfaceView.getResources();
		final float speed = resources.getDisplayMetrics().density * SPEED_MODIFIER;

		// Initialize the renderer
		mGameLogic = new DigitalBirdGameLogic(surfaceView);
		mGameLogic.setGameSpeed(speed);
		mGameLogic.setGameStateListener(this);
		mGameLogic.setPipesInterval(PIPES_INTERVAL);
	}

	public GameState getGameState() {
		return mGameLogic.getGameState();
	}

	public boolean isGameStarted() {
		return mGameLogic.isGameStarted();
	}

	public void setGameListener(final GameListener listener) {
		mGameListener = listener;
	}

	public void reset() {
		mGameLogic.reset();
	}

	public void ready() {
		mGameLogic.ready();
	}

	public void start() {
		mGameLogic.start();
	}

	public void pauseIfStarted() {
		mGameLogic.pause();
	}

	public void resumeIfPaused() {
		mGameLogic.resume();
	}

	public void onButtonClicked() {
		mGameLogic.fly();
	}

	@Override
	public void onPointsScored(final int pointsScored, final int totalPoints) {
		if (mGameListener != null)
			mGameListener.onScore(totalPoints);
	}

	@Override
	public boolean onGameOver(final int totalPoints) {
		return mGameListener != null && mGameListener.onGameOver(totalPoints);
	}
}
