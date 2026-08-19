package view.gameview;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;

/**
 * Reads the playable lawn rectangle from the TMX map.
 *
 * Expected TMX structure:
 *
 * Object Layer:
 *      map
 *
 * Rectangle Object:
 *      pitch
 *
 * The returned rectangle is the single source of truth for:
 *
 * - plant rendering offset
 * - sun rendering offset
 * - mouse -> grid conversion
 * - board cell size calculation
 */
public final class PitchBoundsReader {

    private static final String OBJECT_LAYER_NAME = "map";
    private static final String PITCH_OBJECT_NAME = "pitch";


    private PitchBoundsReader() {
        /*
         * Utility class.
         */
    }


    public static Rectangle read(
        TiledMap tiledMap
    ) {

        if (tiledMap == null) {
            throw new IllegalArgumentException(
                "tiledMap cannot be null"
            );
        }


        MapLayer layer =
            tiledMap
                .getLayers()
                .get(OBJECT_LAYER_NAME);


        if (layer == null) {

            throw new IllegalStateException(
                "Object layer '"
                    + OBJECT_LAYER_NAME
                    + "' was not found in TMX map."
            );
        }


        MapObject object =
            layer
                .getObjects()
                .get(PITCH_OBJECT_NAME);


        if (object == null) {

            throw new IllegalStateException(
                "Object '"
                    + PITCH_OBJECT_NAME
                    + "' was not found in layer '"
                    + OBJECT_LAYER_NAME
                    + "'."
            );
        }


        if (
            !(object instanceof RectangleMapObject rectangleObject)
        ) {

            throw new IllegalStateException(
                "TMX object '"
                    + PITCH_OBJECT_NAME
                    + "' must be a Rectangle Object."
            );
        }


        Rectangle rectangle =
            rectangleObject
                .getRectangle();


        if (
            rectangle.width <= 0f
                || rectangle.height <= 0f
        ) {

            throw new IllegalStateException(
                "Invalid pitch rectangle size: "
                    + rectangle
            );
        }


        /*
         * Return a copy so external code cannot accidentally
         * modify the TMX geometry object.
         */
        return new Rectangle(
            rectangle
        );
    }
}
