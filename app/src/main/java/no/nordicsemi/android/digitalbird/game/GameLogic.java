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

import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/** A basic API for a game. */
public abstract class GameLogic implements SurfaceHolder.Callback {
	private static final String TAG = "GameLogic";

	public interface GameStateListener {
		/**
		 * User has scored points.
		 * @param pointsScored number of points scored
		 * @param totalPoints total number of points since game was started
		 */
		public void onPointsScored(final int pointsScored, final int totalPoints);

		/**
		 * The game is over.
		 * @param totalPoints total number of points since game was started
		 * @return true if the record has been beaten
		 */
		public boolean onGameOver(final int totalPoints);
	}

	/**
	 * The score listener reference.
	 */
	private GameStateListener mGameStateListener;
	/**
	 * The Surface view to draw frames on.
	 */
	private final SurfaceView mSurfaceView;
	/**
	 * The thread that calculates and draws the frames. The frames are then drawn on the SurfaceView in the UI thread.
	 */
	private GameRendererThread mThread;
	/**
	 * The current game state.
	 * @see GameState
	 */
	private GameState mGameState;
	/**
	 * The game speed modifier. This value should be calculated using the screen density to ensure the same speed on each device.
	 */
	private float mGameSpeed;
	/**
	 * Points scored since the game was started.
	 */
	private int mPoints;
	/**
	 * Flag set to true when the record was beaten.
	 */
	private boolean mRecordBeaten;

	/**
	 * Basic constructor of the game logic. Initializes the surface and sets the game state to {@link GameState#INITIALIZING}.
	 * @param surfaceView the surface to draw the game on
	 */
	public GameLogic(final SurfaceView surfaceView) {
		mSurfaceView = surfaceView;
		surfaceView.getHolder().addCallback(this);

		mGameState = GameState.INITIALIZING;
	}

	public final void setGameStateListener(final GameStateListener listener) {
		mGameStateListener = listener;
	}

	/**
	 * Sets the game speed. The higher number, the faster the game will be.
	 * @param speed the game speed modifier
	 */
	public final void setGameSpeed(final float speed) {
		mGameSpeed = speed;
	}

	/**
	 * Returns the game speed modifier. This should be multiplied by the time interval to calculate the objects translation.
	 */
	protected final float getGameSpeed() {
		return mGameSpeed;
	}

	/**
	 * Returns current the game state.
	 * @return the game state
	 */
	public final GameState getGameState() {
		return mGameState;
	}

	/**
	 * Returns true if the game is in {@link GameState#STARTED} state.
	 * @return whether the game is in STARTED state
	 */
	public final boolean isGameStarted() {
		return GameState.STARTED.equals(mGameState);
	}

	/**
	 * Returns true if the record has been beaten.
	 */
	protected boolean isRecordBeaten() {
		return mRecordBeaten;
	}

	/**
	 * Resets the game to {@link GameState#INITIALIZING} state. All animations are stopped but the screen does not clear.
	 */
	public final void reset() {
		mPoints = 0;
		mRecordBeaten = false;
		mGameState = GameState.INITIALIZING;
		onGameInitialized();
	}

	/**
	 * Starts the game if it's not started yet. To resume the paused game use {@link #resume()} method instead.
	 */
	public final void ready() {
		if (GameState.INITIALIZING.equals(mGameState) || GameState.OVER.equals(mGameState)) {
			mPoints = 0;
			mRecordBeaten = false;
			mGameState = GameState.READY;
			onGameReady();
		}
	}

	/**
	 * Changes the state of the game from {@link GameState#READY} to {@link GameState#STARTED}.
	 */
	public final void start() {
		if (GameState.READY.equals(mGameState)) {
			mGameState = GameState.STARTED;
			onGameStarted();
		}
	}

	/**
	 * Resumes the paused game.
	 */
	public final void resume() {
		if (GameState.PAUSED.equals(mGameState)) {
			mGameState = GameState.READY;
			onGameResumed();
		}
	}

	/**
	 * Pauses the game if it has been started. All animations are stopped but the game state is not cleared.
	 */
	public final void pause() {
		if (GameState.STARTED.equals(mGameState)) {
			mGameState = GameState.PAUSED;
			onGamePaused();
		}
	}

	/**
	 * Callback called when the game has been initialized.
	 */
	protected void onGameInitialized() {
		// empty default implementation
	}

	/**
	 * Callback called when the game is ready to be started.
	 */
	protected void onGameReady() {
		// empty default implementation
	}

	/**
	 * Callback called when the game has been started.
	 */
	protected void onGameStarted() {
		// empty default implementation
	}

	/**
	 * Callback called when the game has been resumed.
	 */
	protected void onGameResumed() {
		// empty default implementation
	}

	/**
	 * Callback called when the game has been paused.
	 */
	protected void onGamePaused() {
		// empty default implementation
	}

	/**
	 * Adds given number of points to the total score.
	 * @param points number of points scored this time
	 */
	protected void score(final int points) {
		mPoints += points;
		if (mGameStateListener != null)
			mGameStateListener.onPointsScored(points, mPoints);
	}

	/**
	 * Method called when the is almost over and some final animations need to take place.
	 */
	protected void onGameFinishing() {
		mGameState = GameState.FINISHING;
	}

	/**
	 * Method called when the bird hit the ground.
	 */
	protected void onGameOver() {
		mGameState = GameState.OVER;
		if (mGameStateListener != null)
			mRecordBeaten = mGameStateListener.onGameOver(mPoints);
	}

	/**
	 * Moves all objects according to the deltaTime parameter.
	 * @param deltaTime the delta time since the last frame.
	 * @param state the current game state
	 */
	public abstract void calculateFrame(final long deltaTime, final GameState state);

	/**
	 * Draws a frame on the canvas.
	 * @param canvas the canvas to draw on. This should be acquired using {@link #lockCanvas()} method and released using {@link #unlockCanvasAndPost(Canvas)}.
	 * @param state the current game state
	 */
	public abstract void drawFrame(final Canvas canvas, final GameState state);

	/**
	 * Callback called when the dimensions of the surface has changed.
	 * @param format the new PixelFormat of the surface
	 * @param width the new width of the surface
	 * @param height the new height of the surface
	 */
	public abstract void onSurfaceChanged(final int format, final int width, final int height);

	/**
	 * Locks the renderer surface for drawing. The locked surface is then returned. It must be unlocked using {@link #unlockCanvasAndPost(Canvas)}
	 * in order the changes to be visible for a user.
	 * @return locked canvas
	 */
	public Canvas lockCanvas() {
		return mSurfaceView.getHolder().lockCanvas();
	}

	/**
	 * Unlocks the canvas after drawing a frame.
	 * @param canvas the canvas that has been locked before
	 */
	public void unlockCanvasAndPost(final Canvas canvas) {
		mSurfaceView.getHolder().unlockCanvasAndPost(canvas);
	}

	@Override
	public void surfaceCreated(final SurfaceHolder holder) {
		mThread = new GameRendererThread(this);
		mThread.start();
	}

	@Override
	public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
		onSurfaceChanged(format, width, height);
	}

	@Override
	public void surfaceDestroyed(final SurfaceHolder holder) {
		mThread.killThread();

		// Wait until the thread will die
		boolean retry = true;

		while (retry) {
			try {
				mThread.join();
				retry = false;
			} catch (Exception e) {
				Log.e(TAG, "Destroying surface failed", e);
			}
		}
	}
}
