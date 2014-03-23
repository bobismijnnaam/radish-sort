package nl.plusminos.gdx.bacongame7;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;

//Idea:
/* Food enters the screen at the corners of the screen
* Travels to the center of the screen
* You have to choose a destination to send it to using arrow keys
* Different foods should go in different directions
* First make it so that there are four directions with constant animals
* (So the top position will always be a cow)
* Later on (if I make it that far) animals should be able to change once they've eaten a bit
* The spawning of food should go faster and faster
* If you sent a piece of food in the wrong direction it takes a life away of that animal
* Once an animal has no more lifes left it disappears
* You lose if all animals are gone
* Every correctly chosen piece of food increases the score by a point
* Available foods and animals:
* 		Bacon - *
* 		Carrot - Bunny
* 		Cheese - Mouse
* 		Grass - Cow
* 		Insect - Frog
*/

// Todo-list:
/*
 * - Betere moeilijkheidscurve
 */

public class BaconGame7 implements ApplicationListener, InputProcessor {
	public enum GameState {
		MENU,
		GAME,
		SCORE
	}
	
	public enum SortDirection {
		UP,
		RIGHT,
		DOWN,
		LEFT
	}
	
	public enum FoodState {
		ENTERING, 	// Means it's flying from one of the corners to the middle
		SENT,		// Means it's flying from the middle to one of the animals
		NEXT		// Means it was received by one of the animals and that a new
					// food should be sent
	}
	
	// Constants
	private float SCR_W, SCR_H;	
	private final boolean ENABLE_SOUND = true; 
	
	// Textures
	private Texture menuBg;
	private Texture scoreBg;
	private TextureAtlas ingameAtlas;
	
	// Musics
	private Music gameMusic;
	private Music menuMusic;
	
	// Sounds
	private Sound eatSound;
	private Sound wrongFoodSound;
	private Sound endSound;
	
	// Font
	private FreeTypeFontGenerator fontGenerator;
	private BitmapFont scoreFont;
	
	// Sprites
	private Sprite menuSprite;
	private Sprite scoreSprite;
	
	private Sprite bgSprite;
	private Sprite arrowSprite;
	
	// Eaters!
	private Eater cowEater;
	private Eater bunnyEater;
	private Eater frogEater;
	private Eater mouseEater;
	private final int ID_COW = 0; // TRouBLe (as in css)
	private final int ID_FROG = 1;
	private final int ID_BUNNY = 2;
	private final int ID_MOUSE = 3;
	
	private Sprite[] foodSprites = new Sprite[5];
	private final int SPRITE_INSECT = 0;
	private final int SPRITE_CARROT = 1;
	private final int SPRITE_CHEESE = 2;
	private final int SPRITE_GRASS = 3;
	private final int SPRITE_BACON = 4;
	
	// Game vars
	private GameState gameState;
	private int score;
	private SortDirection sortDir = SortDirection.RIGHT;
	private OrthographicCamera camera;
	private SpriteBatch batch;
	private int[] lifes; // Lifes mapping equals TRouBLe (see css)
	
	// Variables for food flying in the screen
	private FoodState foodState;
	private Vector2 startPos, endPos; // Start/end coordinates
	private long stateStart; // The time the current foodState was changed
	private final long BASE_TRAVEL_TIME = 650; // The minimal of time a state has to take (apart from the NEXT state)
	private final long VARIABLE_START_TIME = 1000; // The variable time you start with
	private final float SHRINK_FACTOR = 0.95f;
	private long variableTravelTime; // The variable time it takes to travel from one place to another, this will decrease over time
	private Sprite foodSprite;
	private int spinDir;
	private SortDirection chosenDir;
	
	@Override
	public void create() {		
		// Initiate screen size variables
		SCR_W = Gdx.graphics.getWidth();
		SCR_H = Gdx.graphics.getHeight();
		
		// Initialize input
		Gdx.input.setInputProcessor(this);
		
		// Initialize font
		fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("data/Pacifico.ttf"));
		
		scoreFont = fontGenerator.generateFont(80);
		scoreFont.setColor(Color.WHITE);
		
		// Initialize camera
		camera = new OrthographicCamera(SCR_W, SCR_H);
		camera.setToOrtho(false, SCR_W, SCR_H);
		batch = new SpriteBatch();
		
		// Initialize music
		gameMusic = Gdx.audio.newMusic(Gdx.files.internal("data/Monkeys Spinning Monkeys.mp3"));
		gameMusic.setLooping(true);
		
		menuMusic = Gdx.audio.newMusic(Gdx.files.internal("data/Broken Reality.mp3"));
		menuMusic.setLooping(true);
		
		// Initialize sound effects
		eatSound = Gdx.audio.newSound(Gdx.files.internal("data/eat.mp3"));
		wrongFoodSound = Gdx.audio.newSound(Gdx.files.internal("data/wrongFood.mp3"));
		endSound = Gdx.audio.newSound(Gdx.files.internal("data/end.mp3"));
		
		// Load textures
		menuBg = new Texture(Gdx.files.internal("data/menu.png"));
		menuBg.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		
		scoreBg = new Texture(Gdx.files.internal("data/score.png"));
		scoreBg.setFilter(TextureFilter.Linear,  TextureFilter.Linear);
		
		// Atlas containing all in-game graphics
		ingameAtlas = new TextureAtlas(Gdx.files.internal("data/ingame.pack"));
		
		// Set up some basic sprites
		menuSprite = new Sprite(menuBg);
		menuSprite.setPosition(0, 0);
		scoreSprite = new Sprite(scoreBg);
		
		// In-game sprites that will be reused
		bgSprite = ingameAtlas.createSprite("bg");
		
		cowEater = new Eater(250, 512, ingameAtlas.createSprite("cow"));
		bunnyEater = new Eater(313, 10, ingameAtlas.createSprite("bunny"));
		frogEater = new Eater(538, 241, ingameAtlas.createSprite("frog"));
		mouseEater = new Eater(24, 254, ingameAtlas.createSprite("mouse"));
		
		arrowSprite = ingameAtlas.createSprite("arrow");
		arrowSprite.setPosition(236, 235);
		arrowSprite.setOrigin(arrowSprite.getWidth() / 2, arrowSprite.getHeight() / 2);
		
		foodSprites[SPRITE_BACON] = ingameAtlas.createSprite("bacon");
		foodSprites[SPRITE_CARROT] = ingameAtlas.createSprite("carrot");
		foodSprites[SPRITE_CHEESE] = ingameAtlas.createSprite("cheese");
		foodSprites[SPRITE_GRASS] = ingameAtlas.createSprite("grass");
		foodSprites[SPRITE_INSECT] = ingameAtlas.createSprite("insect");
		
		setNextState(GameState.MENU);
	}

	@Override
	public void dispose() {
		batch.dispose();
		
		menuBg.dispose();
		scoreBg.dispose();
		ingameAtlas.dispose();
		
		scoreFont.dispose();
		fontGenerator.dispose();
		
		gameMusic.dispose();
		menuMusic.dispose();
		eatSound.dispose();
		wrongFoodSound.dispose();
		endSound.dispose();
	}

	@Override
	public void render() {		
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		
		long dt = TimeUtils.millis() - stateStart;
		long totalTime = BASE_TRAVEL_TIME + variableTravelTime;
		
		switch(gameState) {
			case MENU:
				menuSprite.draw(batch); // Draw menu background
				break;
			case GAME:
				// Logic
				
				// Food logic
				switch (foodState) {
					case NEXT:
						// Set the next state & update delay
						foodState = FoodState.ENTERING;
						variableTravelTime *= SHRINK_FACTOR;
						
						// Select a new type of food
						foodSprite = foodSprites[(int) (Math.random() * 5)];
						
						// Calculate the start/end positions for the first sequence
						// Start: align  corner of food with corner of screen
						switch ((int) (Math.random() * 4)) { // Topleft, topright, bottomright, bottomleft;
							case 0:
								startPos = new Vector2(-foodSprite.getWidth(), SCR_H);
								break;
							case 1:
								startPos = new Vector2(SCR_W, SCR_H);
								break;
							case 2:
								startPos = new Vector2(SCR_W, -foodSprite.getHeight());
								break;
							case 3:
								startPos = new Vector2(-foodSprite.getWidth(), -foodSprite.getHeight());
								break;
						}
						// End: position on center of screen
						endPos = new Vector2((SCR_W - foodSprite.getWidth()) / 2, (SCR_H - foodSprite.getHeight()) / 2);
						
						// Set starting position of sprite to startPos and reset rotation
						foodSprite.setPosition(startPos.x, startPos.y);
						foodSprite.setRotation(0);
						
						// Update (decrease) travel time & start time
						stateStart = TimeUtils.millis();
						
						// Set spinning direction for future use
						spinDir = Math.random() < 0.5 ? 1 : -1;
						
						break;
					case ENTERING:
						// Check if time has been exceeded 
						if (dt > totalTime) {
							// Time has been exceeded
							// Set next state
							foodState = FoodState.SENT;
							
							// Update positions
							startPos = endPos;
							
							// Save sent direction for checking if it was correct!
							chosenDir = sortDir;
							
							switch (sortDir) {
								case DOWN:
									endPos = new Vector2(350, 68);
									break;
								case LEFT:
									endPos = new Vector2(73, 326);
									break;
								case RIGHT:
									endPos = new Vector2(620, 300);
									break;
								case UP:
									endPos = new Vector2(352, 630);
									break;
							}
							endPos.add(foodSprite.getWidth() / -2, foodSprite.getHeight() / -2);
							
							// Update state start time & sprite position
							stateStart = TimeUtils.millis();
							foodSprite.setPosition(startPos.x, startPos.y);
						} else {
							// Time has not yet been exceeded
							// Update position
							double progress = dt / (double) totalTime;
							Vector2 dPos = startPos.cpy().lerp(endPos, (float) progress);
							foodSprite.setPosition(dPos.x, dPos.y);
						}
						break;
					case SENT:
						// Check if time has been exceeded 
						if (dt > totalTime) {
							// Time has been exceeded
							
							// Check if food was sent in the correct direction
							if (foodSprite == foodSprites[SPRITE_BACON]) { // If it was bacon it's okay because BACON
								score++;
								foodState = FoodState.NEXT;
								switch (chosenDir) { // Add one life to the eater of bacon
									case DOWN:
										lifes[ID_BUNNY]++;
										bunnyEater.wiggle();
										break;
									case LEFT:
										lifes[ID_MOUSE]++;
										mouseEater.wiggle();
										break;
									case RIGHT:
										lifes[ID_FROG]++;
										frogEater.wiggle();
										break;
									case UP:
										lifes[ID_COW]++;
										cowEater.wiggle();
										break;
								}
							} else {
								switch (chosenDir) {
									case DOWN:
										if (foodSprite == foodSprites[SPRITE_CARROT]) {
											// Food was correct!
											score++;
											foodState = FoodState.NEXT;
											bunnyEater.wiggle();
										} else {
											// Food was incorrect :(
											lifes[ID_BUNNY]--;
											bunnyEater.shake();
										}
										break;
									case LEFT:
										if (foodSprite == foodSprites[SPRITE_CHEESE]) {
											// Food was correct!
											score++;
											foodState = FoodState.NEXT;
											mouseEater.wiggle();
										} else {
											// Food was incorrect :(
											lifes[ID_MOUSE]--;
											mouseEater.shake();
										}
										break;
									case RIGHT:
										if (foodSprite == foodSprites[SPRITE_INSECT]) {
											// Food was correct!
											score++;
											foodState = FoodState.NEXT;
											frogEater.wiggle();
										} else {
											// Food was incorrect :(
											lifes[ID_FROG]--;
											frogEater.shake();
										}
										break;
									case UP:
										if (foodSprite == foodSprites[SPRITE_GRASS]) {
											// Food was correct!
											score++;
											foodState = FoodState.NEXT;
											cowEater.wiggle();
										} else {
											// Food was incorrect :(
											lifes[ID_COW]--;
											cowEater.shake();
										}
										break;	
								}
							}
							
							if (foodState == FoodState.NEXT) {
								// Food was correct!
								if (ENABLE_SOUND) eatSound.play();
							} else if (foodState == FoodState.SENT) {
								// Unchanged, thus there might've been an incorrect piece of food
								// Check for game over
								boolean gameOver = false;
								for (int i = 0; i < lifes.length; i++) {
									if (lifes[i] == 0) {
										setNextState(GameState.SCORE);
										gameOver = true;
										break;
									}
								}
								
								if (!gameOver) { // If not gameover, spawn another piece of food!
									foodState = FoodState.NEXT;
									
									if (ENABLE_SOUND) wrongFoodSound.play();
								}
							}
							
							// Sanity check
							foodSprite = null;
						} else {
							// Time has not been exceeded
							// Update position & rotation
							
							Vector2 dPos = startPos.cpy().lerp(endPos, (float) (dt / (double) totalTime));
							foodSprite.setPosition(dPos.x, dPos.y);
							foodSprite.rotate((float) (0.5 * Math.PI * spinDir));
						}
						break;
					default:
						break;
					
				}
				
				// Render the scene
				bgSprite.draw(batch);
				
				// cowSprite.draw(batch);
				cowEater.draw(batch);
				bunnyEater.draw(batch);
				frogEater.draw(batch);
				mouseEater.draw(batch);
				arrowSprite.draw(batch);
				
				if (foodSprite != null) {
					foodSprite.draw(batch);
				}
				
				break;
			case SCORE:
				scoreSprite.draw(batch); // Draw score background

				TextBounds tb = scoreFont.getBounds(String.valueOf(score)); // Calculate bounds
				scoreFont.draw(batch, String.valueOf(score), 344 - tb.width / 2, 195 + tb.height / 2); // Draw actual score
				
				break;
		}
		
		batch.end();
	}
	
	private void setNextState(GameState nextState) {
		if (ENABLE_SOUND) {
			if (gameMusic.isPlaying()) {
				gameMusic.stop();
			}
			if (menuMusic.isPlaying()) {
				menuMusic.stop();
			}
		}
		
		// Initialize stuff
		switch (nextState) {
			case MENU:
				if (ENABLE_SOUND) menuMusic.play();
				break; // Won't happen
			case GAME:
				foodState = FoodState.NEXT;
				variableTravelTime = VARIABLE_START_TIME;
				
				score = 0;
				sortDir = SortDirection.RIGHT;
				arrowSprite.setRotation(0);
				
				lifes = new int[]{5, 5, 5, 5};
				
				if (ENABLE_SOUND) gameMusic.play();
				
				cowEater.idle();
				frogEater.idle();
				bunnyEater.idle();
				mouseEater.idle();
				break;
			case SCORE:
				if (ENABLE_SOUND) endSound.play();
				break;
		}
		
		gameState = nextState;
	}

	@Override
	public boolean keyDown(int keycode) {
		switch (gameState) {
			case MENU:
				if (keycode == Input.Keys.ENTER) {
					setNextState(GameState.GAME);
				}
				break;
			case GAME:
				if (keycode == Input.Keys.UP) {
					sortDir = SortDirection.UP;
					arrowSprite.setRotation(90);
				} else if (keycode == Input.Keys.RIGHT) {
					sortDir = SortDirection.RIGHT;
					arrowSprite.setRotation(0);
				} else if (keycode == Input.Keys.DOWN) {
					sortDir = SortDirection.DOWN;
					arrowSprite.setRotation(270);
				} else if (keycode == Input.Keys.LEFT) {
					sortDir = SortDirection.LEFT;
					arrowSprite.setRotation(180);
				} else if (keycode == Input.Keys.SPACE) {
					// score = 865;
					// setNextState(GameState.SCORE);
				}
				break;
			case SCORE:
				if (keycode == Input.Keys.ENTER) {
					setNextState(GameState.GAME);
				}
				break;
		}
		
		return true;
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}
	
	@Override
	public boolean keyUp(int keycode) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		// TODO Auto-generated method stub
		return false;
	}
}
