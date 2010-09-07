package com.jakewharton.tronwallpaper;

import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import com.jakewharton.utilities.WidgetLocationsPreference;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

/**
 * Tron game.
 * 
 * @author Jake Wharton
 */
public class Game implements SharedPreferences.OnSharedPreferenceChangeListener {
	/**
	 * Possible directions of travel.
	 * 
	 * @author Jake Wharton
	 */
	enum Direction {
		NORTH, SOUTH, EAST, WEST;
		
		
		
		/**
		 * Get the direction that is the opposite of this one.
		 * 
		 * @return Opposite direction.
		 */
		public Game.Direction getOpposite() {
			switch (this) {
				case NORTH:
					return Game.Direction.SOUTH;
				case SOUTH:
					return Game.Direction.NORTH;
				case EAST:
					return Game.Direction.WEST;
				case WEST:
					return Game.Direction.EAST;
				default:
					throw new IllegalStateException("This is impossible.");
			}
		}
	}
	
	
	
	/**
	 * Single random number generate for this wallpaper.
	 */
	/*package*/static final Random RANDOM = new Random();
	
	/**
	 * Tag used for logging.
	 */
	private static final String TAG = "TronWallpaper.Game";
	
	/**
	 * Cell value for a wall.
	 */
	private static final boolean CELL_WALL = false;
	
	/**
	 * Cell value for a blank space.
	 */
	private static final boolean CELL_BLANK = true;
	

	
	/**
	 * Number of cells on the board horizontally.
	 */
	private int mCellsWide;
	
	/**
	 * Number of cells on the board vertically.
	 */
	private int mCellsTall;
	
	/**
	 * Number of cells horizontally between the columns.
	 */
	private int mCellColumnSpacing;
	
	/**
	 * Number of cells vertically between the rows.
	 */
	private int mCellRowSpacing;
	
	/**
	 * Width (in pixels) of a single cell.
	 */
	private float mScaleX;
	
	/**
	 * Height (in pixels) of a single cell.
	 */
	private float mScaleY;
	
	/**
	 * Height (in pixels) of the screen.
	 */
    private int mScreenHeight;
    
    /**
     * Width (in pixels) of the screen.
     */
    private int mScreenWidth;
    
    /**
     * Whether or not the screen is currently in landscape mode.
     */
    private boolean mIsLandscape;
    
    /**
     * Number of icon rows on the launcher.
     */
    private int mIconRows;
    
    /**
     * Number of icon columns on the launcher.
     */
    private int mIconCols;
    
    /**
     * 2-dimensional array of the board's cells.
     */
	private boolean[][] mBoard;
    
    /**
     * Color of the background.
     */
    private int mGameBackground;
    
    /**
     * Top padding (in pixels) of the grid from the screen top.
     */
    private float mDotGridPaddingTop;
    
    /**
     * Left padding (in pixels) of the grid from the screen left.
     */
    private float mDotGridPaddingLeft;
    
    /**
     * Bottom padding (in pixels) of the grid from the screen bottom.
     */
    private float mDotGridPaddingBottom;
    
    /**
     * Right padding (in pixels) of the grid from the screen right.
     */
    private float mDotGridPaddingRight;
    
    /**
     * Path to the user background image (if any).
     */
    private String mBackgroundPath;
    
    /**
     * The user background image (if any).
     */
    private Bitmap mBackground;
    
    /**
     * The locations of widgets on the launcher.
     */
    private List<Rect> mWidgetLocations;
    
    /**
     * Paint to draw the background color.
     */
    private final Paint mBackgroundPaint;
    
    /**
     * Walls foreground color.
     */
    private final Paint mWallsForeground;
    
    /**
     * Whether or not we are displaying icon walls
     */
    private boolean mIsDisplayingWalls;
    
    /**
     * Current cycle positions
     */
    private final LinkedList<Point> mLightCycle;
    
    /**
     * Opponent cycle positions
     */
    private final LinkedList<Point> mOpponent;
    
    /**
     * Current snake direction.
     */
    private Game.Direction mDirection;
    
    /**
     * Direction the user wants us to travel in.
     */
    private Game.Direction mWantsToGo;
    
    /**
     * Opponent color.
     */
    private final Paint mOpponentForeground;
    
    /**
     * Light cycle color.
     */
    private final Paint mLightCycleForeground;
    
    
    
    /**
     * Create a new game.
     */
    public Game() {
    	if (Wallpaper.LOG_VERBOSE) {
    		Log.v(Game.TAG, "> Game()");
    	}

        //Create Paints
    	this.mWallsForeground = new Paint(Paint.ANTI_ALIAS_FLAG);
    	this.mWallsForeground.setStyle(Paint.Style.STROKE);
    	this.mWallsForeground.setStrokeWidth(2);
        this.mBackgroundPaint = new Paint();
        this.mOpponentForeground = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mLightCycleForeground = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        this.mLightCycle = new LinkedList<Point>();
        this.mOpponent = new LinkedList<Point>();
        
        //Load all preferences or their defaults
        Wallpaper.PREFERENCES.registerOnSharedPreferenceChangeListener(this);
        this.onSharedPreferenceChanged(Wallpaper.PREFERENCES, null);

    	if (Wallpaper.LOG_VERBOSE) {
    		Log.v(Game.TAG, "< Game()");
    	}
    }

    
    
    /**
     * Handle the changing of a preference.
     */
	public void onSharedPreferenceChanged(final SharedPreferences preferences, final String key) {
    	if (Wallpaper.LOG_VERBOSE) {
    		Log.v(Game.TAG, "> onSharedPreferenceChanged()");
    	}
    	
		final boolean all = (key == null);
		final Resources resources = Wallpaper.CONTEXT.getResources();
		
		boolean hasLayoutChanged = false;
		boolean hasGraphicsChanged = false;
		
		final String showWalls = resources.getString(R.string.settings_display_showwalls_key);
		if (all || key.equals(showWalls)) {
			this.mIsDisplayingWalls = preferences.getBoolean(showWalls, resources.getBoolean(R.bool.display_showwalls_default));
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Game.TAG, "Is Displaying Walls: " + this.mIsDisplayingWalls);
			}
		}
		
		final String widgetLocations = resources.getString(R.string.settings_display_widgetlocations_key);
		if (all || key.equals(widgetLocations)) {
			this.mWidgetLocations = WidgetLocationsPreference.convertStringToWidgetList(preferences.getString(widgetLocations, resources.getString(R.string.display_widgetlocations_default)));
			hasLayoutChanged = true;
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Game.TAG, "Widget Locations: " + (this.mWidgetLocations.size() / 4));
			}
		}
		
		
		// COLORS //
        
		final String gameBackground = resources.getString(R.string.settings_color_background_key);
		if (all || key.equals(gameBackground)) {
			this.mGameBackground = preferences.getInt(gameBackground, resources.getInteger(R.integer.color_background_default));
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Game.TAG, "Background: #" + Integer.toHexString(this.mGameBackground));
			}
		}
		
		final String wallsForeground = resources.getString(R.string.settings_color_walls_key);
		if (all || key.equals(wallsForeground)) {
			this.mWallsForeground.setColor(preferences.getInt(wallsForeground, resources.getInteger(R.integer.color_walls_default)));
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Game.TAG, "Walls Foreground: #" + Integer.toHexString(this.mWallsForeground.getColor()));
			}
		}
		
		final String backgroundImage = resources.getString(R.string.settings_color_bgimage_key);
		if (all || key.equals(backgroundImage)) {
			this.mBackgroundPath = preferences.getString(backgroundImage, null);
			
			if (this.mBackgroundPath != null) {			
				if (Wallpaper.LOG_DEBUG) {
					Log.d(Game.TAG, "Background Image: " + this.mBackgroundPath);
				}
				
				//Trigger performResize
				hasGraphicsChanged = true;
			} else {
				this.mBackground = null;
			}
		}
		
		final String backgroundOpacity = resources.getString(R.string.settings_color_bgopacity_key);
		if (all || key.equals(backgroundOpacity)) {
			this.mBackgroundPaint.setAlpha(preferences.getInt(backgroundOpacity, resources.getInteger(R.integer.color_bgopacity_default)));
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Game.TAG, "Background Image Opacity: " + this.mBackgroundPaint.getAlpha());
			}
		}
		
		final String snake = resources.getString(R.string.settings_color_snake_key);
		if (all || key.equals(snake)) {
			this.mLightCycleForeground.setColor(preferences.getInt(snake, resources.getInteger(R.integer.color_snake_default)));
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Game.TAG, "Snake Foreground: #" + Integer.toHexString(this.mLightCycleForeground.getColor()));
			}
		}
		
		final String apple = resources.getString(R.string.settings_color_apple_key);
		if (all || key.equals(apple)) {
			this.mOpponentForeground.setColor(preferences.getInt(apple, resources.getInteger(R.integer.color_apple_default)));
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Game.TAG, "Apple Foreground: #" + Integer.toHexString(this.mOpponentForeground.getColor()));
			}
		}
    	
        
		// GRID //
		
		final String dotGridPaddingLeft = resources.getString(R.string.settings_display_padding_left_key);
		if (all || key.equals(dotGridPaddingLeft)) {
			this.mDotGridPaddingLeft = preferences.getInt(dotGridPaddingLeft, resources.getInteger(R.integer.display_padding_left_default));
			hasGraphicsChanged = true;
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Game.TAG, "Dot Grid Padding Left: " + this.mDotGridPaddingLeft);
			}
		}

		final String dotGridPaddingRight = resources.getString(R.string.settings_display_padding_right_key);
		if (all || key.equals(dotGridPaddingRight)) {
			this.mDotGridPaddingRight = preferences.getInt(dotGridPaddingRight, resources.getInteger(R.integer.display_padding_right_default));
			hasGraphicsChanged = true;
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Game.TAG, "Dot Grid Padding Right: " + this.mDotGridPaddingRight);
			}
		}

		final String dotGridPaddingTop = resources.getString(R.string.settings_display_padding_top_key);
		if (all || key.equals(dotGridPaddingTop)) {
			this.mDotGridPaddingTop = preferences.getInt(dotGridPaddingTop, resources.getInteger(R.integer.display_padding_top_default));
			hasGraphicsChanged = true;
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Game.TAG, "Dot Grid Padding Top: " + this.mDotGridPaddingTop);
			}
		}

		final String dotGridPaddingBottom = resources.getString(R.string.settings_display_padding_bottom_key);
		if (all || key.equals(dotGridPaddingBottom)) {
			this.mDotGridPaddingBottom = preferences.getInt(dotGridPaddingBottom, resources.getInteger(R.integer.display_padding_bottom_default));
			hasGraphicsChanged = true;
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Game.TAG, "Dot Grid Padding Bottom: " + this.mDotGridPaddingBottom);
			}
		}
		
		
		// CELLS //
		
		final String iconRows = resources.getString(R.string.settings_display_iconrows_key);
		if (all || key.equals(iconRows)) {
			this.mIconRows = preferences.getInt(iconRows, resources.getInteger(R.integer.display_iconrows_default));
			hasLayoutChanged = true;
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Game.TAG, "Icon Rows: " + this.mIconRows);
			}
		}
		
		final String iconCols = resources.getString(R.string.settings_display_iconcols_key);
		if (all || key.equals(iconCols)) {
			this.mIconCols = preferences.getInt(iconCols, resources.getInteger(R.integer.display_iconcols_default));
			hasLayoutChanged = true;
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Game.TAG, "Icon Cols: " + this.mIconCols);
			}
		}
		
		final String cellSpacingRow = resources.getString(R.string.settings_display_rowspacing_key);
		if (all || key.equals(cellSpacingRow)) {
			this.mCellRowSpacing = preferences.getInt(cellSpacingRow, resources.getInteger(R.integer.display_rowspacing_default));
			hasLayoutChanged = true;
			
			if (Wallpaper.LOG_DEBUG) {
		    	Log.d(Game.TAG, "Cell Row Spacing: " + this.mCellRowSpacing);
			}
		}
		
		final String cellSpacingCol = resources.getString(R.string.settings_display_colspacing_key);
		if (all || key.equals(cellSpacingCol)) {
			this.mCellColumnSpacing = preferences.getInt(cellSpacingCol, resources.getInteger(R.integer.display_colspacing_default));
			hasLayoutChanged = true;
			
			if (Wallpaper.LOG_DEBUG) {
		    	Log.d(Game.TAG, "Cell Column Spacing: " + this.mCellColumnSpacing);
			}
		}
		
		if (hasLayoutChanged) {
	    	this.mCellsWide = (this.mIconCols * (mCellColumnSpacing + 1)) + 1;
	    	this.mCellsTall = (this.mIconRows * (mCellRowSpacing + 1)) + 1;
	    	
	    	if (Wallpaper.LOG_DEBUG) {
	    		Log.d(Game.TAG, "Cells Wide: " + this.mCellsWide);
	    		Log.d(Game.TAG, "Cells Tall: " + this.mCellsTall);
	    	}
	    	
	    	//Create playing board
	        this.mBoard = new boolean[this.mCellsTall][this.mCellsWide];
		}
		if (hasLayoutChanged || hasGraphicsChanged) {
			if ((this.mScreenWidth > 0) && (this.mScreenHeight > 0)) {
				//Resize everything to fit
				this.performResize(this.mScreenWidth, this.mScreenHeight);
			}

	    	this.newGame();
		}

    	if (Wallpaper.LOG_VERBOSE) {
    		Log.v(Game.TAG, "< onSharedPreferenceChanged()");
    	}
	}

    /**
     * Test if a Point is a valid coordinate on the game board.
     * 
     * @param position Point representing coordinate.
     * @return Boolean indicating whether or not the position is valid.
     */
	private boolean isValidPosition(final Point position) {
		return ((position.x >= 0) && (position.x < this.mCellsWide)
				&& (position.y >= 0) && (position.y < this.mCellsTall)
				&& (this.mBoard[position.y][position.x] == Game.CELL_BLANK));
	}
    
    /**
     * Reset the game state to that of first initialization.
     */
    public void newGame() {
    	if (Wallpaper.LOG_VERBOSE) {
    		Log.v(Game.TAG, "> newGame()");
    	}

    	//Initialize board
    	final int cellWidth = this.mCellColumnSpacing + 1;
    	final int cellHeight = this.mCellRowSpacing + 1;
    	for (int y = 0; y < this.mCellsTall; y++) {
    		for (int x = 0; x < this.mCellsWide; x++) {
    			this.mBoard[y][x] = ((x % cellWidth == 0) || (y % cellHeight == 0)) ? Game.CELL_BLANK : Game.CELL_WALL;
    		}
    	}
    	
    	//Remove board under widgets
    	for (final Rect widget : this.mWidgetLocations) {
    		if (Wallpaper.LOG_DEBUG) {
    			Log.d(Game.TAG, "Widget: L=" + widget.left + ", T=" + widget.top + ", R=" + widget.right + ", B=" + widget.bottom);
    		}
    		
    		final int left = (widget.left * cellWidth) + 1;
    		final int top = (widget.top * cellHeight) + 1;
    		final int bottom = (widget.bottom * cellHeight) + this.mCellRowSpacing;
    		final int right = (widget.right * cellWidth) + this.mCellColumnSpacing;
    		for (int y = top; y <= bottom; y++) {
    			for (int x = left; x <= right; x++) {
    				this.mBoard[y][x] = Game.CELL_WALL;
    			}
    		}
    	}
    	
    	this.newLife();
    	
    	if (Wallpaper.LOG_VERBOSE) {
    		Log.v(Game.TAG, "< newGame()");
    	}
    }
    
    /**
     * Reset snake.
     */
    private void newLife() {
    	//Create player and opponent
    	this.mLightCycle.clear();
    	this.mOpponent.clear();
    	
    	//TODO: initialize positions and directions
    	
    	this.mWantsToGo = null;
    }
    
    /**
     * Set the user 
     * @param direction
     */
    public void setWantsToGo(final Game.Direction direction) {
    	this.mWantsToGo = direction;
    	
    	if (Wallpaper.LOG_DEBUG) {
    		Log.d(Game.TAG, "Wants To Go: " + direction.toString());
    	}
    }
    
    /**
     * Iterate all entities one step.
     */
    public void tick() {
    	this.determineNextDirection();
    	final Point newPoint = Game.move(this.mLightCycle.getFirst(), this.mDirection);
    	
    	//TODO: move light cycle
    	//TODO: move opponent
    	
    	//TODO: collision check
    }
    
    /**
     * Use line-of-sight to determine next direction of travel.
     */
    private void determineNextDirection() {
		final Point snakeHead = this.mLightCycle.getFirst();
		
		//Try the user direction first
		final Point newPoint = Game.move(snakeHead, this.mWantsToGo);
		if ((this.mWantsToGo != null) && this.isValidPosition(newPoint) && !this.isPointInSnake(newPoint)) {
			//Follow user direction and GTFO
			this.mDirection = this.mWantsToGo;
			return;
		}
		
		//TODO: this
		
		//If the wants-to-go direction exists and the AI forced us to change direction then wants-to-go direction
		//is impossible and should be cleared
		if ((this.mWantsToGo != null) && (this.mDirection != nextDirection)) {
			this.mWantsToGo = null;
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Game.TAG, "Clearing wants-to-go direction via AI.");
			}
		}
		
		this.mDirection = nextDirection;
    }

    /**
     * Resize the game board and all entities according to a new width and height.
     * 
     * @param screenWidth New width.
     * @param screenHeight New height.
     */
    public void performResize(int screenWidth, int screenHeight) {
    	if (Wallpaper.LOG_VERBOSE) {
    		Log.v(Game.TAG, "> performResize(width = " + screenWidth + ", height = " + screenHeight + ")");
    	}
    	
    	//Background image
    	if (this.mBackgroundPath != null) {
			try {
				final Bitmap temp = BitmapFactory.decodeStream(Wallpaper.CONTEXT.getContentResolver().openInputStream(Uri.parse(this.mBackgroundPath)));
				final float pictureAR = temp.getWidth() / (temp.getHeight() * 1.0f);
				final float screenAR = screenWidth / (screenHeight * 1.0f);
				int newWidth;
				int newHeight;
				int x;
				int y;
				
				if (pictureAR > screenAR) {
					//wider than tall related to the screen AR
					newHeight = screenHeight;
					newWidth = (int)(temp.getWidth() * (screenHeight / (temp.getHeight() * 1.0f)));
					x = (newWidth - screenWidth) / 2;
					y = 0;
				} else {
					//taller than wide related to the screen AR
					newWidth = screenWidth;
					newHeight = (int)(temp.getHeight() * (screenWidth / (temp.getWidth() * 1.0f)));
					x = 0;
					y = (newHeight - screenHeight) / 2;
				}
				
	    		final Bitmap scaled = Bitmap.createScaledBitmap(temp, newWidth, newHeight, false);
	    		this.mBackground = Bitmap.createBitmap(scaled, x, y, screenWidth, screenHeight);
			} catch (FileNotFoundException e) {
				Toast.makeText(Wallpaper.CONTEXT, "Unable to load background bitmap. File not found.", Toast.LENGTH_LONG);
				e.printStackTrace();
				Log.w(Game.TAG, "Unable to load background bitmap. File not found.");
				this.mBackground = null;
			} catch (OutOfMemoryError e) {
				Toast.makeText(Wallpaper.CONTEXT, "Unable to load background bitmap. Not enough memory.", Toast.LENGTH_LONG);
				e.printStackTrace();
				Log.w(Game.TAG, "Unable to load background bitmap. Not enough memory.");
				this.mBackground = null;
			} catch (NullPointerException e) {
				Toast.makeText(Wallpaper.CONTEXT, "Unable to load background bitmap.", Toast.LENGTH_LONG);
				e.printStackTrace();
				Log.w(Game.TAG, "Unable to load background bitmap. Null pointer exception.");
				this.mBackground = null;
			}
    	}
    	
    	if (screenWidth > screenHeight) {
    		this.mIsLandscape = true;
    		final int temp = screenHeight;
    		screenHeight = screenWidth;
    		screenWidth = temp;
    	} else {
    		this.mIsLandscape = false;
    	}
    	
    	this.mScreenWidth = screenWidth;
    	this.mScreenHeight = screenHeight;
    	
    	if (this.mIsLandscape) {
    		this.mScaleX = (screenWidth - this.mDotGridPaddingTop) / (this.mCellsWide * 1.0f);
    		this.mScaleY = (screenHeight - (this.mDotGridPaddingBottom + this.mDotGridPaddingLeft + this.mDotGridPaddingRight)) / (this.mCellsTall * 1.0f);
    	} else {
    		this.mScaleX = (screenWidth - (this.mDotGridPaddingLeft + this.mDotGridPaddingRight)) / (this.mCellsWide * 1.0f);
    		this.mScaleY = (screenHeight - (this.mDotGridPaddingTop + this.mDotGridPaddingBottom)) / (this.mCellsTall * 1.0f);
    	}
    	
    	if (Wallpaper.LOG_DEBUG) {
    		Log.d(Game.TAG, "Is Landscape: " + this.mIsLandscape);
    		Log.d(Game.TAG, "Screen Width: " + screenWidth);
    		Log.d(Game.TAG, "Screen Height: " + screenHeight);
    		Log.d(Game.TAG, "Scale X: " + this.mScaleX);
    		Log.d(Game.TAG, "Scale Y: " + this.mScaleY);
    	}

    	if (Wallpaper.LOG_VERBOSE) {
    		Log.v(Game.TAG, "< performResize()");
    	}
    }
    
    /**
     * Render the board and all entities on a Canvas.
     * 
     * @param c Canvas to draw on.
     */
    public void draw(final Canvas c) {
    	c.save();
    	
    	//Clear the screen in case of transparency in the image
		c.drawColor(this.mGameBackground);
    	if (this.mBackground != null) {
    		//Bitmap should already be sized to the screen so draw it at the origin
    		c.drawBitmap(this.mBackground, 0, 0, this.mBackgroundPaint);
    	}
        
        if (this.mIsLandscape) {
        	//Perform counter-clockwise rotation
        	c.rotate(-90, this.mScreenWidth / 2.0f, this.mScreenWidth / 2.0f);
        	c.translate(0, this.mDotGridPaddingLeft);
        } else {
        	c.translate(this.mDotGridPaddingLeft, this.mDotGridPaddingTop);
        }
        c.scale(this.mScaleX, this.mScaleY);
        
        //Draw dots and walls
        this.drawGameBoard(c);

        if (this.mIsLandscape) {
        	//Perform clockwise rotation back to normal
        	c.rotate(90, this.mScreenWidth / 2.0f, this.mScreenWidth / 2.0f);
        }
        
        c.restore();
    }

    /**
     * Render the dots and walls.
     * 
     * @param c Canvas to draw on.
     */
    private void drawGameBoard(final Canvas c) {
    	//draw light cycle
    	for (final Point position : this.mLightCycle) {
    		final float left = position.x;
    		final float top = position.y;
    		final float right = left + 1;
    		final float bottom = top + 1;
    		c.drawRect(left, top, right, bottom, this.mLightCycleForeground);
    	}
    	//draw opponent
    	for (final Point position : this.mOpponent) {
    		final float left = position.x;
    		final float top = position.y;
    		final float right = left + 1;
    		final float bottom = top + 1;
    		c.drawRect(left, top, right, bottom, this.mOpponentForeground);
    	}
    	
        //draw walls if enabled
        if (this.mIsDisplayingWalls) {
        	for (int y = 0; y < this.mIconRows; y++) {
        		for (int x = 0; x < this.mIconCols; x++) {
        			float left = (x * (this.mCellColumnSpacing + 1)) + 1;
        			float top = (y * (this.mCellRowSpacing + 1)) + 1;
        			float right = left + this.mCellColumnSpacing;
        			float bottom = top + this.mCellRowSpacing;
        			
        			c.drawRect(left, top, right, bottom, this.mWallsForeground);
        			
        			left += Game.CELL_OVER_EIGHT;
        			top += Game.CELL_OVER_EIGHT;
        			right -= Game.CELL_OVER_EIGHT;
        			bottom -= Game.CELL_OVER_EIGHT;
        			
        			c.drawRect(left, top, right, bottom, this.mWallsForeground);
        		}
        	}
        }
    }

    

	/**
	 * Update the point one step in the direction specified.
	 * 
	 * @param point Point of original coordinates.
	 * @param direction Direction in which to move the point.
	 * @return New point coordinates.
	 */
    private static Point move(final Point point, final Game.Direction direction) {
    	final Point newPoint = new Point(point);
    	if (direction != null) {
	    	switch (direction) {
	    		case NORTH:
	    			newPoint.y -= 1;
					break;
					
	    		case SOUTH:
	    			newPoint.y += 1;
					break;
					
	    		case WEST:
	    			newPoint.x -= 1;
					break;
					
	    		case EAST:
	    			newPoint.x += 1;
					break;
	    	}
    	}
    	return newPoint;
    }
    
    /**
     * Determine whether two points represent the same coordinate.
     * 
     * @param one Point one.
     * @param two Point two.
     * @return Boolean.
     */
    private static boolean pointEquals(final Point one, final Point two) {
    	return ((one.x == two.x) && (one.y == two.y));
    }
}