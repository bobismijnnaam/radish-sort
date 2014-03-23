package nl.plusminos.gdx.bacongame7;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class Main {
	public static void main(String[] args) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "Radish Sort";
		cfg.width = 700;
		cfg.height = 700;
		cfg.resizable = false;
		
		new LwjglApplication(new BaconGame7(), cfg);
	}
}
