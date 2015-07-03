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
import android.graphics.Canvas;
import android.os.Build;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.LinkedList;
import java.util.Queue;

import no.nordicsemi.android.digitalbird.game.graphics.Background;
import no.nordicsemi.android.digitalbird.game.graphics.DigitalBird;
import no.nordicsemi.android.digitalbird.game.graphics.Fireworks;
import no.nordicsemi.android.digitalbird.game.graphics.Pipe;
import no.nordicsemi.android.digitalbird.game.graphics.Plane;
import no.nordicsemi.android.digitalbird.game.graphics.StatusBar;

/**
 * This class is responsible for both the game logic and managing objects on the surface view.
 */
public class DigitalBirdGameLogic extends GameLogic implements SurfaceHolder.Callback {
	/** Object used for access synchronization. */
	private final Object mLock = new Object();
	/**
	 * The interval between two pipes in milliseconds.
	 * There are 4 pipes available than can be on the screen simultaneously. If the interval is too short some may be skipped.
	 */
	private long mPipeInterval;
	/**
	 * The total time since the last pipe has been added. When the delay reach the {@link #mPipeInterval} a new pipe is added and the delay is reset.
	 */
	private long mPipeDelay;

	// Game objects
	/** The background. The background contains also the moving grass, therefore if has the {@link Background#move(float)} method. */
	private Background mBackground;
	/** On Android Lollipop the status bar may also be drawn. This object will color if to a darker color. */
	private StatusBar mStatusBar;
	/** A queue of pipes that may be used for drawing. There is a limited number of pipes available. */
	private Queue<Pipe> mPipes;
	/** A queue of pipes added to the screen. */
	private Queue<Pipe> mVisiblePipes;
	/** The Digital Bird, of course :) */
	private DigitalBird mBird;
	/** The Plane */
	private Plane mPlane;
	/** Fireworks animation. */
	private Fireworks mFireworks;

	public DigitalBirdGameLogic(final SurfaceView surfaceView) {
		super(surfaceView);

		initGameObjects(surfaceView.getResources());
	}

	/**
	 * Sets the interval between two pipes in milliseconds. There are 4 available pipes that can be drawn simultaneously. If the interval is too
	 * short some of them may be skipped.
	 * @param interval the interval between two pipes in milliseconds.
	 */
	public void setPipesInterval(final long interval) {
		mPipeInterval = interval;
	}

	@Override
	protected void onGameReady() {
		synchronized (mLock) {
			mPipes.addAll(mVisiblePipes);
			mVisiblePipes.clear();
		}
		mBird.reset();
		mFireworks.reset();

		// Reset the pipe timer so that a new one will pop up in a fixed period of time.
		mPipeDelay = 0L;
	}

	@Override
	protected void onGameStarted() {
		// User has started the game with a button. We also want the bird to start flying at this moment.
		mBird.fly();
	}

	/**
	 * Causes the bird to fly a little. This method should be called when the user clicks the button or the screen.
	 * If game is not in {@link GameState#STARTED STARTED} state this method does nothing.
	 */
	public void fly() {
		if (isGameStarted())
			mBird.fly();
	}

	/**
	 * This method initializes all static game objects.
	 * @param resources application resources
	 */
	private void initGameObjects(final Resources resources) {
		mBackground = new Background(resources);
		mBird = new DigitalBird(resources);
		mPlane = new Plane(resources);
		mFireworks = new Fireworks(resources);

		// Initialize 2 pipes. We will use only 2 of them, in most cases, but just to be sure we add more if the pipe interval was very small.
		mPipes = new LinkedList<>();
		mPipes.add(new Pipe(resources));
		mPipes.add(new Pipe(resources));
		mPipes.add(new Pipe(resources));
		mPipes.add(new Pipe(resources));

		// Pipes must be moved to this list in order to be drawn on the screen.
		mVisiblePipes = new LinkedList<>();

		// The status bar may be drawn only on devices with Android Lollipop or newer.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			mStatusBar = new StatusBar(resources);
		}
	}

	@Override
	public void calculateFrame(final long deltaTime, final GameState state) {
		final DigitalBird bird = mBird; // for optimization

		// Calculate the translation based on the interval between two frames and the game speed.
		final float scaledDeltaTime = deltaTime * getGameSpeed();

		switch (state) {
			case STARTED:
				mPipeDelay += deltaTime;

				// First check if any pipe can't be removed
				synchronized (mLock) {
					for (final Pipe pipe : mVisiblePipes) {
						if (pipe.isPipeReadyToRelease()) {
							mPipes.add(mVisiblePipes.poll());
							break;
						}
					}

					// Add a new pipe if the time has came
					if (mPipeDelay > mPipeInterval) {
						final Pipe pipe = mPipes.poll();
						if (pipe != null)
							mVisiblePipes.add(pipe.reset());
						mPipeDelay = 0L;
					}

					// Move all pipes that are visible
					for (final Pipe pipe : mVisiblePipes) {
						pipe.move(scaledDeltaTime);
						if (pipe.isPipeScored(bird))
							score(1);
					}

					// Check the bird's collision with pipes
					for (final Pipe pipe : mVisiblePipes)
						if (pipe.checkCollision(bird)) {
							onGameFinishing();
							break;
						}
				}

				// Move the grass in the background
				mBackground.move(scaledDeltaTime);

				// Fly, Digital Bird, fly!
				bird.move(scaledDeltaTime);
				mPlane.move(scaledDeltaTime);
				if (!bird.isAlive()) {
					onGameOver();
					break;
				}
				break;
			case READY:
				// In the initializing state the grass is moving and the bird is flying straight.
				mBackground.move(scaledDeltaTime);
				bird.move(scaledDeltaTime);
				mPlane.move(scaledDeltaTime);
				break;
			case FINISHING:
				mPlane.move(scaledDeltaTime);
				// When Digital Bird hits the pipe, it falls down until hits the ground.
				if (bird.isAlive())
					bird.move(scaledDeltaTime);
				else
					onGameOver();
				break;
			case OVER:
				mPlane.move(scaledDeltaTime);
				mFireworks.move(scaledDeltaTime);
				break;
		}
	}

	@Override
	public void drawFrame(final Canvas canvas, final GameState state) {
		// The order of drawing is important. First the background, to clear the previous frame, then fireworks, pipes, the bird, and finally status bar on Lolliopo only.
		mBackground.draw(canvas);
		if (isRecordBeaten() && GameState.OVER.equals(state))
			mFireworks.draw(canvas);
		mPlane.draw(canvas);
		synchronized (mLock) {
			for (final Pipe pipe : mVisiblePipes)
				pipe.draw(canvas);
		}
		mBird.draw(canvas);
		if (mStatusBar != null)
			mStatusBar.draw(canvas);


		// Other user controls are handled by the Activity, not the GameLogic.
	}

	@Override
	public void onSurfaceChanged(int format, int width, int height) {
		mBackground.setScreenDimensions(width, height);
		final int worldHeight = mBackground.getGroundPositionY();
		for (final Pipe pipe : mPipes)
			pipe.setScreenDimensions(width, worldHeight);
		mBird.setScreenDimensions(width, worldHeight);
		mPlane.setScreenDimensions(width, worldHeight);
		mFireworks.setScreenDimensions(width, worldHeight);

		if (mStatusBar != null)
			mStatusBar.setScreenDimensions(width, height);
	}
}
