package otm.harjoitustyo.graphics;

import static org.lwjgl.opengl.GL45.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL45.GL_FLOAT;
import static org.lwjgl.opengl.GL45.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL45.GL_TRIANGLES;
import static org.lwjgl.opengl.GL45.glBindBuffer;
import static org.lwjgl.opengl.GL45.glBindVertexArray;
import static org.lwjgl.opengl.GL45.glBufferData;
import static org.lwjgl.opengl.GL45.glDeleteBuffers;
import static org.lwjgl.opengl.GL45.glDrawArrays;
import static org.lwjgl.opengl.GL45.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL45.glGenBuffers;
import static org.lwjgl.opengl.GL45.glGenVertexArrays;
import static org.lwjgl.opengl.GL45.glVertexAttribPointer;


import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class Sprite implements Drawable {

	private static final float[] VERTICES = {
		// posX, posY, texX, texY
		0.0f, 1.0f, 0.0f, 1.0f,
		1.0f, 0.0f, 1.0f, 0.0f,
		0.0f, 0.0f, 0.0f, 0.0f,

		0.0f, 1.0f, 0.0f, 1.0f,
		1.0f, 1.0f, 1.0f, 1.0f,
		1.0f, 0.0f, 1.0f, 0.0f
	};

	private float rotation = 0; // Angle in degrees
	private Vector4f color;
	private Vector2f scale, position;
	private Texture texture;
	private int vao, vbo;
	private int z = 0;
	private float size;
	private ShaderProgram shaderProgram;
	public boolean deleted = false;

	public Sprite(Texture texture) {
		this.shaderProgram = ShaderManager.spriteShader;
		this.texture = texture;
		position = new Vector2f();
		color = new Vector4f(1);
		setSize(1);
		initGL();
	}

	public Sprite(Texture texture, ShaderProgram shaderProgram) {
		this.shaderProgram = shaderProgram;
		this.texture = texture;
		position = new Vector2f();
		color = new Vector4f(1);
		setSize(1);
		initGL();
	}

	public void setSize(float size) {
		this.size = size;
		if(texture.getHeight() > texture.getWidth()) {
			scale = new Vector2f(1.0f * texture.getWidth() / texture.getHeight(), 1);
		} else {
			scale = new Vector2f(1, 1.0f * texture.getHeight() / texture.getWidth());
		}
		scale.mul(size);
	}

	public void setScale(int x, int y) {
		this.scale = new Vector2f(x, y);
	}

	public int getZ() {
		return z;
	}

	public void setTexture(Texture texture) {
		this.texture = texture;
		setSize(size);
	}

	public Texture getTexture() {
		return texture;
	}

	// r, g, b should be between 0-255
	public void setColor(int r, int g, int b, int a) {
		this.color = new Vector4f(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
	}

	public void setZ(int z) {
		this.z = z;
	}

	public Vector2f getScale() {
		return scale;
	}

	public void setPosition(float x, float y) {
		position = new Vector2f(x, y);
	}

	public void move(float x, float y) {
		position.add(x, y);
	}

	public void setRotation(float angles) {
		rotation = angles % 360;
	}

	public void rotate(float angle) {
		this.rotation += angle;
		this.rotation %= 360;
	}

	private void initGL() {
		vao = glGenVertexArrays();
		vbo = glGenBuffers();

		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, VERTICES, GL_STATIC_DRAW);

		glBindVertexArray(vao);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 4, GL_FLOAT, false, 4 * 4, 0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}

	public void delete() {
		if(!deleted) {
			glDeleteBuffers(vao);
			glDeleteBuffers(vbo);
			texture.references--;
			deleted = true;
		}
	}

	@Override
	public void draw() {
		ShaderManager.getInstance().useShader(shaderProgram);

		Matrix4f model = new Matrix4f();
		model.translate(position.x, position.y, 0);

		model.translate(0.5f * scale.x, 0.5f * scale.y, 0);
		model.rotate(rotation / 180.0f * (float) Math.PI, 0, 0, 1);
		model.translate(-0.5f * scale.x, -0.5f * scale.y, 0);

		model.scale(scale.x, scale.y, 1);

		shaderProgram.setUniformMatrix4f("model", model);
		shaderProgram.setUniformVector4f("spriteColor", color);

		TextureManager.getInstance().useTexture(texture);

		glBindVertexArray(vao);
		glDrawArrays(GL_TRIANGLES, 0, 6);
		glBindVertexArray(0);
	}
}
