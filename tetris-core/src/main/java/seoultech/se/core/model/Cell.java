package seoultech.se.core.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import seoultech.se.core.model.enumType.Color;

@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Cell {
    private Color color = Color.NONE;
    private boolean isOccupied = false;


    // Factory Methods 1~4.
    public static Cell of(Color color) {
        return Cell.of(color, true);
    }

    public static Cell of(Color color, boolean isOccupied) {
        Cell cell = new Cell();
        cell.setColor(color);
        cell.setOccupied(isOccupied);
        return cell;
    }
    
    public Cell copy() {
        return Cell.of(this.color, this.isOccupied);
    }
    
    public static Cell empty() {
        return Cell.of(Color.NONE, false);
    }

    public void clear() {
        this.color = Color.NONE;
        this.isOccupied = false;
    }
}
