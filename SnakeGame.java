import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class SnakeGame extends JPanel implements ActionListener, KeyListener {
    private class Tile {
        int x;
        int y;

        Tile(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }  

    int boardWidth;
    int boardHeight;
    int tileSize = 25;
    
    //snake
    Tile snakeHead;
    ArrayList<Tile> snakeBody;

    //food
    Tile food;
    Random random;

    //game logic
    int velocityX;
    int velocityY;
    Timer gameLoop;
    boolean gameOver = false;
    boolean paused = false;
    int initialDelay = 100;
    int currentDelay;
    int lastScore = -1;

    //background image
    private Image backgroundImage;

    //power-ups
    Tile powerUp;
    boolean powerUpActive = false;
    boolean speedBoostActive = false;
    boolean scoreMultiplierActive = false;
    long powerUpSpawnTime;
    long powerUpActivationTime;
    final long POWER_UP_DURATION = 5000; // 5 seconds
    final long POWER_UP_DESPAWN_TIME = 10000; // 10 seconds
    int scoreMultiplier = 1;

    //high score
    int highScore = 0;

    SnakeGame(int boardWidth, int boardHeight) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        setPreferredSize(new Dimension(this.boardWidth, this.boardHeight));
        addKeyListener(this);
        setFocusable(true);

        snakeHead = new Tile(5, 5);
        snakeBody = new ArrayList<Tile>();

        food = new Tile(10, 10);
        random = new Random();
        placeFood();

        velocityX = 1;
        velocityY = 0;
        
        //load background image
        try {
            backgroundImage = ImageIO.read(new File("SnakeBG.jpeg"));
        } catch (IOException e) {
            System.err.println("Error loading background image: " + e.getMessage());
            backgroundImage = null;
            setBackground(Color.black);
        }

        //game timer
        currentDelay = initialDelay;
        gameLoop = new Timer(currentDelay, this);
        gameLoop.start();
    }    
    
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        // Draw background image
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, boardWidth, boardHeight, null);
        }

        // Draw semi-transparent dark overlay to make elements pop
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRect(0, 0, boardWidth, boardHeight);

        // Draw subtle grid with transparency
        g.setColor(new Color(255, 255, 255, 50));
        for(int i = 0; i < boardWidth/tileSize; i++) {
            g.drawLine(i*tileSize, 0, i*tileSize, boardHeight);
            g.drawLine(0, i*tileSize, boardWidth, i*tileSize); 
        }

        //Food
        g.setColor(Color.MAGENTA);
        g.fillOval(food.x*tileSize, food.y*tileSize, tileSize, tileSize);

        //Power-Up
        if (powerUp != null && !powerUpActive) {
            g.setColor(speedBoostActive ? Color.ORANGE : Color.YELLOW);
            int[] xPoints = {powerUp.x*tileSize + tileSize/2, powerUp.x*tileSize + tileSize, powerUp.x*tileSize + tileSize/2, powerUp.x*tileSize};
            int[] yPoints = {powerUp.y*tileSize, powerUp.y*tileSize + tileSize/2, powerUp.y*tileSize + tileSize, powerUp.y*tileSize + tileSize/2};
            g.fillPolygon(xPoints, yPoints, 4);
        }

        //Snake Head
        g.setColor(Color.CYAN);
        g.fillOval(snakeHead.x*tileSize, snakeHead.y*tileSize, tileSize, tileSize);
        
        //Snake Body
        g.setColor(Color.CYAN);
        for (int i = 0; i < snakeBody.size(); i++) {
            Tile snakePart = snakeBody.get(i);
            g.fillOval(snakePart.x*tileSize, snakePart.y*tileSize, tileSize, tileSize);
        }

        //Score, difficulty, power-up status, and high score with shadow
        int difficultyLevel = calculateDifficultyLevel();
        g.setFont(new Font("Arial", Font.BOLD, 16));
        if (gameOver) {
            // Shadow
            g.setColor(Color.BLACK);
            g.drawString("Game Over: " + String.valueOf(snakeBody.size()), tileSize - 15, tileSize + 1);
            g.drawString("High Score: " + highScore, tileSize - 15, tileSize + 21);
            g.drawString("Press R to Restart", tileSize - 15, tileSize + 41);
            // Main text
            g.setColor(Color.RED);
            g.drawString("Game Over: " + String.valueOf(snakeBody.size()), tileSize - 16, tileSize);
            g.drawString("High Score: " + highScore, tileSize - 16, tileSize + 20);
            g.drawString("Press R to Restart", tileSize - 16, tileSize + 40);
        }
        else if (paused) {
            // Shadow
            g.setColor(Color.BLACK);
            g.drawString("Paused - Press P to Resume", tileSize - 15, tileSize + 41);
            g.drawString("Score: " + String.valueOf(snakeBody.size()) + " | Level: " + difficultyLevel + " | High Score: " + highScore, tileSize - 15, tileSize + 1);
            if (powerUpActive) {
                g.drawString(getPowerUpStatus(), tileSize - 15, tileSize + 21);
            }
            // Main text
            g.setColor(Color.YELLOW);
            g.drawString("Paused - Press P to Resume", tileSize - 16, tileSize + 40);
            g.setColor(Color.WHITE);
            g.drawString("Score: " + String.valueOf(snakeBody.size()) + " | Level: " + difficultyLevel + " | High Score: " + highScore, tileSize - 16, tileSize);
            if (powerUpActive) {
                g.setColor(speedBoostActive ? Color.ORANGE : Color.YELLOW);
                g.drawString(getPowerUpStatus(), tileSize - 16, tileSize + 20);
            }
        }
        else {
            // Shadow
            g.setColor(Color.BLACK);
            g.drawString("Score: " + String.valueOf(snakeBody.size()) + " | Level: " + difficultyLevel + " | High Score: " + highScore, tileSize - 15, tileSize + 1);
            if (powerUpActive) {
                g.drawString(getPowerUpStatus(), tileSize - 15, tileSize + 21);
            }
            // Main text
            g.setColor(Color.WHITE);
            g.drawString("Score: " + String.valueOf(snakeBody.size()) + " | Level: " + difficultyLevel + " | High Score: " + highScore, tileSize - 16, tileSize);
            if (powerUpActive) {
                g.setColor(speedBoostActive ? Color.ORANGE : Color.YELLOW);
                g.drawString(getPowerUpStatus(), tileSize - 16, tileSize + 20);
            }
        }
    }

    private String getPowerUpStatus() {
        long timeLeft = (POWER_UP_DURATION - (System.currentTimeMillis() - powerUpActivationTime)) / 1000 + 1;
        if (speedBoostActive) {
            return "Speed Boost: " + timeLeft + "s";
        } else if (scoreMultiplierActive) {
            return "Score Multiplier: " + timeLeft + "s";
        }
        return "";
    }

    private int calculateDifficultyLevel() {
        int score = snakeBody.size();
        int level = score / 5 + 1;
        return Math.min(level, 10);
    }

    private void updateGameSpeed() {
        int score = snakeBody.size();
        if (score != lastScore) {
            lastScore = score;
            int level = calculateDifficultyLevel();
            int newDelay = Math.max(50, initialDelay - (level - 1) * 5);
            if (speedBoostActive) {
                newDelay = newDelay / 2; // Double speed during speed boost
            }
            if (newDelay != currentDelay) {
                currentDelay = newDelay;
                gameLoop.setDelay(currentDelay);
            }
        }
    }

    private void spawnPowerUp() {
        // 10% chance to spawn a power-up when food is eaten
        if (random.nextInt(100) < 10) {
            int x, y;
            do {
                x = random.nextInt(boardWidth/tileSize);
                y = random.nextInt(boardHeight/tileSize);
            } while (collision(new Tile(x, y), food) || collisionWithSnake(new Tile(x, y)));
            powerUp = new Tile(x, y);
            powerUpSpawnTime = System.currentTimeMillis();
            // Randomly choose power-up type
            speedBoostActive = random.nextBoolean();
        }
    }

    private boolean collisionWithSnake(Tile tile) {
        if (collision(tile, snakeHead)) return true;
        for (Tile snakePart : snakeBody) {
            if (collision(tile, snakePart)) return true;
        }
        return false;
    }

    private void updatePowerUp() {
        // Despawn power-up if it hasn't been collected
        if (powerUp != null && !powerUpActive && System.currentTimeMillis() - powerUpSpawnTime > POWER_UP_DESPAWN_TIME) {
            powerUp = null;
            speedBoostActive = false;
        }
        // Deactivate power-up after duration
        if (powerUpActive && System.currentTimeMillis() - powerUpActivationTime > POWER_UP_DURATION) {
            powerUpActive = false;
            speedBoostActive = false;
            scoreMultiplierActive = false;
            scoreMultiplier = 1;
            updateGameSpeed(); // Reset speed if speed boost ends
        }
    }

    public void placeFood(){
        food.x = random.nextInt(boardWidth/tileSize);
        food.y = random.nextInt(boardHeight/tileSize);
    }

    public void move() {
        //eat food
        if (collision(snakeHead, food)) {
            snakeBody.add(new Tile(food.x, food.y));
            placeFood();
            updateGameSpeed();
            spawnPowerUp();
        }

        //eat power-up
        if (powerUp != null && !powerUpActive && collision(snakeHead, powerUp)) {
            powerUpActive = true;
            powerUpActivationTime = System.currentTimeMillis();
            if (speedBoostActive) {
                updateGameSpeed(); // Apply speed boost
            } else {
                scoreMultiplierActive = true;
                scoreMultiplier = 2; // Double points per food
            }
            powerUp = null;
        }

        //move snake body
        for (int i = snakeBody.size()-1; i >= 0; i--) {
            Tile snakePart = snakeBody.get(i);
            if (i == 0) {
                snakePart.x = snakeHead.x;
                snakePart.y = snakeHead.y;
            }
            else {
                Tile prevSnakePart = snakeBody.get(i-1);
                snakePart.x = prevSnakePart.x;
                snakePart.y = prevSnakePart.y;
            }
        }
        //move snake head
        snakeHead.x += velocityX;
        snakeHead.y += velocityY;

        //game over conditions
        for (int i = 0; i < snakeBody.size(); i++) {
            Tile snakePart = snakeBody.get(i);
            if (collision(snakeHead, snakePart)) {
                gameOver = true;
            }
        }

        if (snakeHead.x*tileSize < 0 || snakeHead.x*tileSize > boardWidth ||
            snakeHead.y*tileSize < 0 || snakeHead.y*tileSize > boardHeight ) {
            gameOver = true;
        }

        updatePowerUp();
    }

    public boolean collision(Tile tile1, Tile tile2) {
        return tile1.x == tile2.x && tile1.y == tile2.y;
    }

    public void restart() {
        // Update high score
        int finalScore = snakeBody.size();
        if (finalScore > highScore) {
            highScore = finalScore;
        }

        // Reset snake
        snakeHead = new Tile(5, 5);
        snakeBody.clear();
        
        // Reset food
        placeFood();
        
        // Reset direction
        velocityX = 1;
        velocityY = 0;
        
        // Reset game state
        gameOver = false;
        paused = false;
        
        // Reset difficulty
        lastScore = -1;
        currentDelay = initialDelay;
        gameLoop.setDelay(currentDelay);
        
        // Reset power-ups
        powerUp = null;
        powerUpActive = false;
        speedBoostActive = false;
        scoreMultiplierActive = false;
        scoreMultiplier = 1;
        
        // Restart game loop
        if (!gameLoop.isRunning()) {
            gameLoop.start();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver && !paused) {
            move();
        }
        repaint();
    }  

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver && e.getKeyCode() == KeyEvent.VK_R) {
            restart();
        }
        else if (!gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_P) {
                paused = !paused;
            }
            else if (!paused) {
                if (e.getKeyCode() == KeyEvent.VK_UP && velocityY != 1) {
                    velocityX = 0;
                    velocityY = -1;
                }
                else if (e.getKeyCode() == KeyEvent.VK_DOWN && velocityY != -1) {
                    velocityX = 0;
                    velocityY = 1;
                }
                else if (e.getKeyCode() == KeyEvent.VK_LEFT && velocityX != 1) {
                    velocityX = -1;
                    velocityY = 0;
                }
                else if (e.getKeyCode() == KeyEvent.VK_RIGHT && velocityX != -1) {
                    velocityX = 1;
                    velocityY = 0;
                }
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
}