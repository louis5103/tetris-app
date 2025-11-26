package seoultech.se.core.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import seoultech.se.core.engine.item.ItemType;
import seoultech.se.core.model.enumType.Color;

/**
 * 게임 보드의 셀
 * 
 * Phase 2 확장:
 * - itemMarker 필드 추가: 줄 삭제 아이템('L') 지원
 */
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Cell {
    private Color color = Color.NONE;
    private boolean isOccupied = false;
    
    /**
     * 아이템 마커 (줄 삭제 아이템용)
     * null이면 일반 블록, ItemType이 있으면 아이템 마커 포함
     */
    private ItemType itemMarker = null;


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
    
    /**
     * 셀 복사 (itemMarker 포함)
     * 
     * @return 복사된 셀
     */
    public Cell copy() {
        Cell cell = Cell.of(this.color, this.isOccupied);
        cell.setItemMarker(this.itemMarker);
        return cell;
    }
    
    public static Cell empty() {
        return Cell.of(Color.NONE, false);
    }

    public void clear() {
        this.color = Color.NONE;
        this.isOccupied = false;
        this.itemMarker = null;
    }
    
    // ========== 아이템 마커 관련 메서드 ==========
    
    /**
     * 아이템 마커가 있는지 확인
     * 
     * @return 아이템 마커가 있으면 true
     */
    public boolean hasItemMarker() {
        return itemMarker != null;
    }
    
    /**
     * 아이템 마커 제거
     */
    public void clearItemMarker() {
        this.itemMarker = null;
    }
    
    /**
     * 아이템 마커를 포함한 셀 생성
     * 
     * @param color 색상
     * @param isOccupied 점유 여부
     * @param itemMarker 아이템 마커
     * @return 새로운 셀
     */
    public static Cell of(Color color, boolean isOccupied, ItemType itemMarker) {
        Cell cell = new Cell();
        cell.setColor(color);
        cell.setOccupied(isOccupied);
        cell.setItemMarker(itemMarker);
        return cell;
    }
}
