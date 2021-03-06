package otm.harjoitustyo.level;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_J;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_K;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;


import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import org.lwjgl.opengl.GL11;
import otm.harjoitustyo.audio.AudioManager;
import otm.harjoitustyo.graphics.Drawable;
import otm.harjoitustyo.graphics.Renderer;
import otm.harjoitustyo.graphics.ShaderManager;
import otm.harjoitustyo.graphics.Sprite;
import otm.harjoitustyo.graphics.Texture;
import otm.harjoitustyo.graphics.TextureManager;
import otm.harjoitustyo.graphics.VideoDecoder;
import otm.harjoitustyo.graphics.text.Text;

public class LevelManager implements Scene {

	private Level level;
	private long startTime, duration;
	boolean running;
	long score = 0, combo = 0;

	private VideoDecoder videoDecoder;

	private Text scoreText, comboText;

	PriorityQueue<TimedEvent> timedEvents = new PriorityQueue<>();

	Sprite background;
	Sprite[] levelEventSprites;

	int levelEventPointer = 0; // points to the index of the next keypress event in the level

	Sprite[] keyPressIndicators;

	int[] heldEventAccuracy = new int[4];
	LevelEvent[] heldEvent = new LevelEvent[4]; // KEY_HOLD event associated with current keydown hold
	Sprite[] heldEventSprite = new Sprite[4];

	List<Drawable> drawables; // List of drawables to delete in end of the level

	// Locations of keypress indicators
	int[] keyX = {530, 530 + 55, 530 + 55 * 2, 530 + 55 * 3};
	String[] keyLabel = {"D", "F", "J", "K"};
	int keyY = 40;

	public LevelManager(Level level) {
		this.level = level;
		drawables = new ArrayList<>();
	}

	public boolean isRunning() {
		return running;
	}

	// accuracy = missed by x milliseconds, will change sprites color according to the accuracy
	public long eventScore(int accuracy, Sprite sprite) {
		accuracy = Math.abs(accuracy);
		if(accuracy > 350) {
			setCombo(0);
			if(sprite != null) {
				sprite.setColor(200, 200, 200, 120);
			}
			return 0;
		}
		setCombo(combo + 1);
		if(accuracy <= 50) {
			if(sprite != null) {
				sprite.setColor(0, 255, 0, 120);
			}
			return 300 * combo;
		} else if(accuracy <= 100) {
			if(sprite != null) {
				sprite.setColor(100, 255, 0, 120);
			}
			return 250 * combo;
		} else if(accuracy <= 150) {
			if(sprite != null) {
				sprite.setColor(200, 255, 0, 120);
			}
			return 150 * combo;
		} else if(accuracy <= 200) {
			if(sprite != null) {
				sprite.setColor(255, 255, 0, 120);
			}
			return 100 * combo;
		} else if(accuracy <= 350) {
			if(sprite != null) {
				sprite.setColor(255, 0, 0, 120);
			}
			return 50 * combo;
		}
		return 0;
	}

	public void loadLevel() {
		level.init();
		this.duration = AudioManager.getInstance().loadFile(level.musicPath).getDuration();

		if(level.backgroundType.equals("video")) {
			videoDecoder = new VideoDecoder(level.backgroundPath);
			Thread videoThread = new Thread(videoDecoder);
			videoThread.start();
		} else {
			throw new Error("Background not implemented.");
		}
		TextureManager.getInstance().setTexture("frame", new Texture("background_loading.png"));
		background = new Sprite(TextureManager.getInstance().getTexture("frame"), ShaderManager.frameShader);
		background.setZ(-1000);
		background.setSize(1280);
		Renderer.getInstance().addDrawable(background);

		keyPressIndicators = new Sprite[4];
		for(int i = 0; i < 4; i++) {
			keyPressIndicators[i] = new Sprite(TextureManager.getInstance().getFileTexture("bb.png"));
			drawables.add(keyPressIndicators[i]);
			keyPressIndicators[i].setSize(50);
			keyPressIndicators[i].setPosition(keyX[i], keyY);
			keyPressIndicators[i].setColor(250, 170, 100, 240);
			keyPressIndicators[i].setZ(10);
			Renderer.getInstance().addDrawable(keyPressIndicators[i]);
			Text txt = new Text(keyLabel[i], "OpenSans-Regular.ttf", 14, 1, 1005);
			txt.setPosition(keyX[i] + 5, keyY + 15);
			Renderer.getInstance().addDrawable(txt);
			drawables.add(txt);
			txt.setColor(255, 255, 255, 255);
		}

		levelEventSprites = new Sprite[level.levelEvents.length];
		for(int i = 0; i < level.levelEvents.length; i++) {
			final int currentIndex = i;
			Sprite sprite = new Sprite(TextureManager.getInstance().getFileTexture("bb.png"));
			timedEvents.add(new TimedEvent((long) Math.floor(level.levelEvents[i].time - 720 / level.scrollingSpeed)) {
				@Override
				public void run() {
					if(level.levelEvents[currentIndex].type == LevelEventType.KEY_HOLD) {
						levelEventSprites[currentIndex] = sprite;
						levelEventSprites[currentIndex].setColor(0, 50, 150, 120);
						int ySize = (int) (level.levelEvents[currentIndex].duration * level.scrollingSpeed);
						levelEventSprites[currentIndex].setScale(50, ySize);
					} else if(level.levelEvents[currentIndex].type == LevelEventType.KEY_PRESS) {
						levelEventSprites[currentIndex] = sprite;
						levelEventSprites[currentIndex].setColor(0, 50, 150, 120);
						levelEventSprites[currentIndex].setScale(50, 15);
					}
					Renderer.getInstance().addDrawable(sprite);
				}
			});
			timedEvents.add(new TimedEvent(Math.min(duration - 100, (long) Math.floor(level.levelEvents[i].time + (sprite.getScale().y + 200) / level.scrollingSpeed))) {
				@Override
				public void run() {
					Renderer.getInstance().deleteDrawable(sprite);
					sprite.delete();
					levelEventSprites[currentIndex] = null;
				}
			});
		}

		scoreText = new Text("0", "OpenSans-Regular.ttf", 72, 1, 1001);
		scoreText.setColor(0, 0, 0, 255);
		scoreText.setPosition(50, 80);
		Renderer.getInstance().addDrawable(scoreText);

		comboText = new Text(combo + "x", "OpenSans-Regular.ttf", 72, 1, 1001);
		comboText.setPosition(1000, 80);
		comboText.setColor(0, 0, 0, 255);
		Renderer.getInstance().addDrawable(comboText);

		AudioManager.getInstance().playAudio(level.musicPath);
		startTime = System.currentTimeMillis();

		running = true;
	}

	public void loop() {
		long now = System.currentTimeMillis();
		if(now - startTime > this.duration) {
			finishLevel();
			return;
		}

		// levelEventPointer points to the index of the next keypress event in the level, may be length+1th element
		while(levelEventPointer < level.levelEvents.length && level.levelEvents[levelEventPointer].time < now - startTime) {
			levelEventPointer++;
		}

		// Process timed events
		while(!timedEvents.isEmpty() && timedEvents.peek().time <= now - startTime) {
			timedEvents.poll().run();
		}

		// Update background
		if(level.backgroundType.equals("video")) {
			synchronized(videoDecoder) {
				if(!videoDecoder.ready) {
					try {
						videoDecoder.wait();
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}

				Texture texture = new Texture(videoDecoder.y, videoDecoder.height, videoDecoder.width, GL11.GL_RED, GL11.GL_RED);
				texture.loadTexture(videoDecoder.u, 1, videoDecoder.height / 2, videoDecoder.width / 2, GL11.GL_RED);
				texture.loadTexture(videoDecoder.v, 2, videoDecoder.height / 2, videoDecoder.width / 2, GL11.GL_RED);
				TextureManager.getInstance().setTexture("frame", texture);
				background.setTexture(TextureManager.getInstance().getTexture("frame"));

				int expectedFrame = (int) Math.floor(((now - startTime) / 1000.0) * videoDecoder.frameRate);
				if(expectedFrame > videoDecoder.currentFrame) {
					if(expectedFrame - videoDecoder.currentFrame > 2) {
						videoDecoder.skipNFrames += expectedFrame - videoDecoder.currentFrame;
					}
					videoDecoder.notifyAll();
				}
			}
		}

		for(int i = 0; i < levelEventSprites.length; i++) {
			if(levelEventSprites[i] != null) {
				levelEventSprites[i].setPosition(keyX[level.levelEvents[i].key], -(now - startTime - level.levelEvents[i].time) * level.scrollingSpeed + 90);
			}
		}
	}

	// Called to assign new score to the score text
	private void updateScoreText() {
		Renderer.getInstance().deleteDrawable(scoreText);
		scoreText.delete();
		scoreText = new Text(Long.toString(score), "OpenSans-Regular.ttf", 72, 1, 1001);
		scoreText.setPosition(50, 80);
		scoreText.setColor(0, 0, 0, 255);
		Renderer.getInstance().addDrawable(scoreText);
	}

	private void setCombo(long combo) {
		this.combo = combo;
		Renderer.getInstance().deleteDrawable(comboText);
		comboText.delete();
		comboText = new Text(combo + "x", "OpenSans-Regular.ttf", 72, 1, 1001);
		comboText.setPosition(1000, 80);
		comboText.setColor(0, 0, 0, 255);
		Renderer.getInstance().addDrawable(comboText);
	}

	private void pressKey(int key) {
		long now = System.currentTimeMillis();

		while(levelEventPointer < level.levelEvents.length && level.levelEvents[levelEventPointer].time < now - startTime) {
			levelEventPointer++;
		}

		keyPressIndicators[key].setColor(250, 50, 250, 240);

		int future = levelEventPointer, past = levelEventPointer - 1, closestEventIndex = -1;
		LevelEvent closestEvent = null;
		while(future < level.levelEvents.length) {
			if((level.levelEvents[future].type == LevelEventType.KEY_HOLD || level.levelEvents[future].type == LevelEventType.KEY_PRESS) && level.levelEvents[future].key == key && !level.levelEvents[future].consumed) {
				closestEvent = level.levelEvents[future];
				closestEventIndex = future;
				break;
			}
			future++;
		}
		while(past >= 0) {
			if((level.levelEvents[past].type == LevelEventType.KEY_HOLD || level.levelEvents[past].type == LevelEventType.KEY_PRESS) && level.levelEvents[past].key == key && !level.levelEvents[past].consumed) {
				if(closestEvent == null || (now - startTime) - level.levelEvents[past].time < closestEvent.time - (now - startTime)) {
					closestEvent = level.levelEvents[past];
					closestEventIndex = past;
				}
				break;
			}
			past--;
		}
		if(closestEvent != null && Math.abs((now - startTime) - closestEvent.time) < 1500) {
			if(closestEvent.type == LevelEventType.KEY_PRESS) {
				score += eventScore(Math.round(closestEvent.time - (now - startTime)), levelEventSprites[closestEventIndex]);
				updateScoreText();

			} else if(closestEvent.type == LevelEventType.KEY_HOLD) {
				heldEvent[key] = closestEvent;
				heldEventSprite[key] = levelEventSprites[closestEventIndex];
				heldEventAccuracy[key] = Math.round(closestEvent.time - (now - startTime));
			}
			closestEvent.consumed = true;
		}
	}

	private void releaseKey(int key) {
		long now = System.currentTimeMillis();
		keyPressIndicators[key].setColor(250, 170, 100, 240);
		if(heldEvent[key] != null) {
			if(Math.abs(now - startTime - (heldEvent[key].time + heldEvent[key].duration)) <= 1500) {
				score += eventScore(heldEventAccuracy[key], heldEventSprite[key]);
				updateScoreText();
			}
			heldEvent[key] = null;
		}
	}


	public Scene nextScene() {
		if(!running) {
			return new HighscoreScreen(level.name, score);
		} else {
			return this;
		}
	}

	public void handleCharInput(long window, int codepoint) {
	}

	public void handleKeyInput(long window, int key, int scancode, int action, int mods) {
		if(running) {
			if(key == GLFW_KEY_D) {
				if(action == GLFW_PRESS) {
					pressKey(0);
				} else if(action == GLFW_RELEASE) {
					releaseKey(0);
				}
			} else if(key == GLFW_KEY_F) {
				if(action == GLFW_PRESS) {
					pressKey(1);
				} else if(action == GLFW_RELEASE) {
					releaseKey(1);
				}
			} else if(key == GLFW_KEY_J) {
				if(action == GLFW_PRESS) {
					pressKey(2);
				} else if(action == GLFW_RELEASE) {
					releaseKey(2);
				}
			} else if(key == GLFW_KEY_K) {
				if(action == GLFW_PRESS) {
					pressKey(3);
				} else if(action == GLFW_RELEASE) {
					releaseKey(3);
				}
			}
		}
	}

	public void finishLevel() {
		running = false;
		synchronized(videoDecoder) {
			videoDecoder.stop = true;
			videoDecoder.notifyAll();
		}

		Renderer.getInstance().deleteDrawable(background);
		background.delete();

		Renderer.getInstance().deleteDrawable(scoreText);
		Renderer.getInstance().deleteDrawable(comboText);
		scoreText.delete();
		comboText.delete();

		drawables.stream().forEach((drawable) -> {
			Renderer.getInstance().deleteDrawable(drawable);
			drawable.delete();
		});
	}

	private class TimedEvent implements Comparable, Runnable {
		public long time;

		public TimedEvent(long time) {
			this.time = time;
		}

		@Override
		public void run() {
		}

		@Override
		public int compareTo(Object o) {
			return Long.compare(time, ((TimedEvent) o).time);
		}
	}
}
