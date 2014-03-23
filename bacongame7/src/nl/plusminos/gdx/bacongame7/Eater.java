package nl.plusminos.gdx.bacongame7;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;

// TODO: Fading red when shaking!

public class Eater {
	public enum EaterState {
		IDLE,
		SHAKING,
		EATING
	}
	
	public final int DURATION_SHAKE = 1000;
	public final int SHAKE_RADIUS = 20;
	public final int SHAKE_PERIODS = 2;
	public final int DURATION_WIGGLE = 1000;
	public final int WIGGLE_RADIUS = 20;
	public final int WIGGLE_PERIODS = 2;

	public Eater(int x, int y, Sprite sprite) {
		rootPos = new Vector2(x, y);
		this.sprite = sprite;
		this.sprite.setPosition(x,  y);
		this.sprite.setOrigin(this.sprite.getWidth() / 2, 0);
	}
	
	private Vector2 rootPos;
	private Sprite sprite;
	private EaterState state = EaterState.IDLE;
	private long stateStart;
	
	public void draw(SpriteBatch batch) {
		int dt = (int) (TimeUtils.millis() - stateStart);
		
		Vector2 dPos = new Vector2(rootPos.x, rootPos.y);
		int newRot = 0;
		
		switch (state) {
			case IDLE: // Nothing to declare
				break;
			case EATING:
				if (dt >= DURATION_WIGGLE) {
					idle();
				} else {
					int singleWiggle = DURATION_WIGGLE / WIGGLE_PERIODS;
					float p = (dt % singleWiggle) / (float) singleWiggle;
					if (p < 0.5) {
						p = Math.abs(4f * (p - 0.25f)) - 1f; // Plot with Wolfram|ALPHA if you want to know what this does
					} else {
						p = (Math.abs(4f * (p - 0.75f)) - 1f) * -1f; // Same here
					}
					
					newRot = (int) (p * WIGGLE_RADIUS);
				}
				break;
			case SHAKING:
				if (dt >= DURATION_SHAKE) {
					idle(); // Reset to standard
				} else {
					float clamp = 0.9f; // Clamp factor
					float p = dt / (float) DURATION_SHAKE; // Calculate how far shaking has progressed on [0, 1] 
					p = (float) (-Math.pow((p - 0.5f) * 2f, 2f) + 1f); // Plot with Wolfram|ALPHA if you want to know what this parabola looks like
					p = p > clamp ? clamp : p; // Clamp to the maximum value
					p /= clamp; // Normalize
					sprite.setColor(1, 1 - p, 1 - p, 1); // Set the color according to parabola
					
					int singleShake = DURATION_SHAKE / SHAKE_PERIODS;
					p = (dt % singleShake) / (float) singleShake;
					if (p < 0.5) {
						p = Math.abs(4f * (p - 0.25f)) - 1f; // Idem
					} else {
						p = (Math.abs(4f * (p - 0.75f)) - 1f) * -1f; // Dito
					}
					
					dPos = rootPos.cpy().add(p * SHAKE_RADIUS, 0);
				}
				break;
		}
		
		sprite.setPosition(dPos.x, dPos.y);
		sprite.setRotation(newRot);
		
		sprite.draw(batch);
	}
	
	public void shake() { // SHake horizontally, turn red!
		state = EaterState.SHAKING;
		stateStart = TimeUtils.millis();
		
		sprite.setPosition(rootPos.x, rootPos.y);
		sprite.setRotation(0);
		sprite.setColor(1, 1, 1, 1);
	}
	
	public void wiggle() { // Wiggle around origin
		state = EaterState.EATING;
		stateStart = TimeUtils.millis();
		
		sprite.setPosition(rootPos.x, rootPos.y);
		sprite.setRotation(0);
		sprite.setColor(1, 1, 1, 1);
	}
	
	public void idle() {
		state = EaterState.IDLE;
		sprite.setPosition(rootPos.x, rootPos.y);
		sprite.setRotation(0);
		sprite.setColor(1, 1, 1, 1);
	}

}
