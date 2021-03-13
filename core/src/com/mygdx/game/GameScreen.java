package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.LinkedList;
import java.util.ListIterator;

public class GameScreen implements Screen{


    private static final float TOUCH_MOVEMENT_THRESHOLD = 0.5f;
    //screen
    private Camera camera;
    private Viewport viewport;

    //graphics
    private SpriteBatch batch;
    private TextureAtlas textureAtlas;

    //private Texture background;
    private TextureRegion[] backgrounds;
    private TextureRegion playerShipTextureRegion, playerShieldTextureRegion, enemyShipTextureRegion, enemyShieldTextureRegion,
            playerLaserTextureRegion, enemyLaserTextureRegion;


    //timing
    //private int backgroundOffset;
    private float[] backgroundOffsets = {0,0,0,0};
    private float backgroundMaxScrollingSpeed;

    //world parameters
    private final int WORLD_WIDTH=72;
    private final int WORLD_HEIGHT=128;

    //game objects
    private PlayerShip playerShip;
    private EnemyShip enemyShip;
    private LinkedList<Laser> playerLaserList;
    private LinkedList<Laser> enemyLaserList;

    GameScreen(){
        camera = new OrthographicCamera();
        viewport = new StretchViewport(WORLD_WIDTH,WORLD_HEIGHT,camera);
/*        background = new Texture("darkPurpleStarscape.png");
        backgroundOffset = 0;*/
        Music music = Gdx.audio.newMusic(Gdx.files.internal("audio.mp3"));
        music.play();
        music.setLooping(true);

        //set up texture atlas
        textureAtlas=new TextureAtlas("images.atlas");

        backgrounds=new TextureRegion[4];
        backgrounds[0]=textureAtlas.findRegion("Starscape00");
        backgrounds[1]=textureAtlas.findRegion("Starscape01");
        backgrounds[2]=textureAtlas.findRegion("Starscape02");
        backgrounds[3]=textureAtlas.findRegion("Starscape03");

        backgroundMaxScrollingSpeed = (float)(WORLD_HEIGHT)/4;

        //initialize texture regions
        playerShipTextureRegion=textureAtlas.findRegion("Ship3");
        enemyShipTextureRegion=textureAtlas.findRegion("Ship2");
        playerShieldTextureRegion=textureAtlas.findRegion("shield2");
        enemyShieldTextureRegion=textureAtlas.findRegion("shield1");
        enemyShieldTextureRegion.flip(false,true);
        playerLaserTextureRegion=textureAtlas.findRegion("laserBlue13");
        enemyLaserTextureRegion=textureAtlas.findRegion("laserRed03");

        //set up game objects
        playerShip=new PlayerShip(48,6,18,18, 0.4f, 4, 50, 0.2f,
                WORLD_WIDTH/2, WORLD_HEIGHT/4, playerShipTextureRegion, playerShieldTextureRegion, playerLaserTextureRegion);

        enemyShip=new EnemyShip((int)(MyGdxGame.random.nextFloat()*25)+25,2,16,16, 0.3f, 5, 55, 0.7f,
                MyGdxGame.random.nextFloat()*(WORLD_WIDTH-10)+5, WORLD_HEIGHT-5, enemyShipTextureRegion, enemyShieldTextureRegion, enemyLaserTextureRegion);

        playerLaserList = new LinkedList<Laser>();
        enemyLaserList = new LinkedList<Laser>();

        batch = new SpriteBatch();
    }


    @Override
    public void show() {

    }

    @Override
    public void render(float deltaTime) {
        batch.begin();

        detectInput(deltaTime);

        moveEnemies(deltaTime);

        playerShip.update(deltaTime);
        enemyShip.update(deltaTime);

        //scroll
        renderBackground(deltaTime);

        //enemy ships
        enemyShip.draw(batch);
        //player ships
        playerShip.draw(batch);

        //lasers
        renderLasers(deltaTime);

        //detect laser/ship collisions
        detectCollisions();

        //explosions
        renderExplosions(deltaTime);

        batch.end();
    }

    private void moveEnemies(float deltaTime){
        //check max distance, randomize

        float leftLimit, rightLimit, upLimit, downLimit;
        leftLimit = -enemyShip.boundingBox.x;
        downLimit = (float)WORLD_HEIGHT/2-enemyShip.boundingBox.y;
        rightLimit = WORLD_WIDTH-enemyShip.boundingBox.x-enemyShip.boundingBox.width;
        upLimit = WORLD_HEIGHT-enemyShip.boundingBox.y-enemyShip.boundingBox.height;

        float xMove = enemyShip.getDirectionVector().x * enemyShip.movementSpeed * deltaTime;
        float yMove = enemyShip.getDirectionVector().y * enemyShip.movementSpeed * deltaTime;

        if (xMove > 0) xMove = Math.min(xMove, rightLimit);
        else xMove = Math.max(xMove,leftLimit);

        if (yMove > 0) yMove = Math.min(yMove, upLimit);
        else yMove = Math.max(yMove,downLimit);

        enemyShip.translate(xMove,yMove);
    }

    private void detectInput(float deltaTime){
        //keyboard input

        float leftLimit, rightLimit, upLimit, downLimit;
        leftLimit = -playerShip.boundingBox.x;
        downLimit = -playerShip.boundingBox.y;
        rightLimit = WORLD_WIDTH-playerShip.boundingBox.x-playerShip.boundingBox.width;
        upLimit = (float)WORLD_HEIGHT/2-playerShip.boundingBox.y-playerShip.boundingBox.height;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) && rightLimit>0){
            playerShip.translate(Math.min(playerShip.movementSpeed*deltaTime, rightLimit), 0f);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP) && upLimit>0){
            playerShip.translate(0f, Math.min(playerShip.movementSpeed*deltaTime, upLimit));
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) && leftLimit<0){
            playerShip.translate(Math.max(-playerShip.movementSpeed*deltaTime, leftLimit), 0f);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) && downLimit<0){
            playerShip.translate(0f, Math.max(-playerShip.movementSpeed*deltaTime, downLimit));
        }

        //touch/mouse input
        if (Gdx.input.isTouched()) {
            //get the screen position of the touch
            float xTouchPixels = Gdx.input.getX();
            float yTouchPixels = Gdx.input.getY();

            //convert to world position
            Vector2 touchPoint = new Vector2(xTouchPixels, yTouchPixels);
            touchPoint = viewport.unproject(touchPoint);

            //calculate the x and y differences
            Vector2 playerShipCentre = new Vector2(
                    playerShip.boundingBox.x + playerShip.boundingBox.width/2,
                    playerShip.boundingBox.y + playerShip.boundingBox.height/2);

            float touchDistance = touchPoint.dst(playerShipCentre);

            if (touchDistance > TOUCH_MOVEMENT_THRESHOLD) {
                float xTouchDifference = touchPoint.x - playerShipCentre.x;
                float yTouchDifference = touchPoint.y - playerShipCentre.y;

                //scale to the maximum speed of the ship
                float xMove = xTouchDifference / touchDistance * playerShip.movementSpeed * deltaTime;
                float yMove = yTouchDifference / touchDistance * playerShip.movementSpeed * deltaTime;

                if (xMove > 0) xMove = Math.min(xMove, rightLimit);
                else xMove = Math.max(xMove,leftLimit);

                if (yMove > 0) yMove = Math.min(yMove, upLimit);
                else yMove = Math.max(yMove,downLimit);

                playerShip.translate(xMove,yMove);
            }
        }
    }

    private void detectCollisions(){
        //check if player laser intersects enemy ship
        ListIterator<Laser> iterator = playerLaserList.listIterator();
        while(iterator.hasNext()) {
            Laser laser = iterator.next();
        if (enemyShip.intersects(laser.getBoundingBox())){
            //contact with enemy ship
            enemyShip.hit(laser);
            iterator.remove();
        }
        }
        //check if enemy laser intersects player ship
        iterator = enemyLaserList.listIterator();
        while(iterator.hasNext()) {
            Laser laser = iterator.next();
            if (playerShip.intersects(laser.getBoundingBox())){
                //contact with enemy ship
                playerShip.hit(laser);
                iterator.remove();
            }
        }
    }

    private void renderExplosions(float deltaTime){

    }

    private void renderLasers(float deltaTime){
        if (playerShip.canFireLaser()){
            Laser[] lasers = playerShip.fireLasers();
            for (Laser laser: lasers){
                playerLaserList.add(laser);
            }
        }
        //enemy lasers
        if (enemyShip.canFireLaser()){
            Laser[] lasers = enemyShip.fireLasers();
            for (Laser laser: lasers){
                enemyLaserList.add(laser);
            }
        }
        //draw
        //remove old
        ListIterator<Laser> iterator = playerLaserList.listIterator();
        while(iterator.hasNext()) {
            Laser laser = iterator.next();
            laser.draw(batch);
            laser.yPosition += laser.movementSpeed*deltaTime;
            if (laser.yPosition > WORLD_HEIGHT) {
                iterator.remove();
            }
        }
        iterator = enemyLaserList.listIterator();
        while(iterator.hasNext()) {
            Laser laser = iterator.next();
            laser.draw(batch);
            laser.yPosition -= laser.movementSpeed*deltaTime;
            if (laser.yPosition + laser.height < 0) {
                iterator.remove();
            }
        }
    }

    private void renderBackground(float deltaTime){
        backgroundOffsets[0] += deltaTime*backgroundMaxScrollingSpeed/8;
        backgroundOffsets[1] += deltaTime*backgroundMaxScrollingSpeed/4;
        backgroundOffsets[2] += deltaTime*backgroundMaxScrollingSpeed/2;
        backgroundOffsets[3] += deltaTime*backgroundMaxScrollingSpeed;

        for (int layer=0;layer<backgroundOffsets.length;layer++){
            if (backgroundOffsets[layer]>WORLD_HEIGHT){
                backgroundOffsets[layer]=0;
            }
            batch.draw(backgrounds[layer],0,-backgroundOffsets[layer],WORLD_WIDTH,WORLD_HEIGHT);
            batch.draw(backgrounds[layer],0,-backgroundOffsets[layer]+WORLD_HEIGHT,WORLD_WIDTH,WORLD_HEIGHT);
        }

    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width,height,true);
        batch.setProjectionMatrix(camera.combined);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}
