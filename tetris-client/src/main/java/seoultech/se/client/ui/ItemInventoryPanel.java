package seoultech.se.client.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import seoultech.se.core.item.Item;

/**
 * 아이템 인벤토리 UI 패널
 * 
 * 기능:
 * - 획득한 아이템 표시
 * - 아이템 사용 버튼
 * - 최대 인벤토리 크기 제한
 * 
 * 사용 예시:
 * ItemInventoryPanel panel = new ItemInventoryPanel(3);
 * panel.addItem(bombItem);
 * panel.setOnItemUse(item -> gameController.useItem(item));
 */
public class ItemInventoryPanel extends HBox {
    
    /**
     * 아이템 인벤토리
     */
    private final List<Item> inventory;
    
    /**
     * 최대 인벤토리 크기
     */
    private final int maxSize;
    
    /**
     * 아이템 사용 콜백
     */
    private ItemUseCallback onItemUse;
    
    /**
     * 아이템 슬롯 UI 컴포넌트
     */
    private final List<VBox> itemSlots;
    
    /**
     * 아이템 사용 콜백 인터페이스
     */
    @FunctionalInterface
    public interface ItemUseCallback {
        void onUse(Item item, int slotIndex);
    }
    
    /**
     * 생성자
     * 
     * @param maxSize 최대 인벤토리 크기
     */
    public ItemInventoryPanel(int maxSize) {
        this.maxSize = maxSize;
        this.inventory = new ArrayList<>();
        this.itemSlots = new ArrayList<>();
        
        initializeUI();
    }
    
    /**
     * UI 초기화
     */
    private void initializeUI() {
        setSpacing(10);
        setAlignment(Pos.CENTER);
        getStyleClass().add("item-inventory");
        
        // 최대 크기만큼 슬롯 생성
        for (int i = 0; i < maxSize; i++) {
            VBox slot = createEmptySlot(i);
            itemSlots.add(slot);
            getChildren().add(slot);
        }
    }
    
    /**
     * 빈 슬롯 생성
     * 
     * @param index 슬롯 인덱스
     * @return 빈 슬롯 VBox
     */
    private VBox createEmptySlot(int index) {
        VBox slot = new VBox(5);
        slot.setAlignment(Pos.CENTER);
        slot.getStyleClass().add("item-slot");
        slot.getStyleClass().add("empty");
        slot.setPrefSize(80, 100);
        slot.setMaxSize(80, 100);
        slot.setMinSize(80, 100);
        
        // 빈 슬롯 표시
        Label emptyLabel = new Label("━");
        emptyLabel.getStyleClass().add("item-empty");
        emptyLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: #666;");
        
        Label slotNumberLabel = new Label(String.valueOf(index + 1));
        slotNumberLabel.getStyleClass().add("slot-number");
        slotNumberLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #999;");
        
        slot.getChildren().addAll(emptyLabel, slotNumberLabel);
        
        return slot;
    }
    
    /**
     * 아이템 슬롯 생성
     * 
     * @param item 아이템
     * @param index 슬롯 인덱스
     * @return 아이템 슬롯 VBox
     */
    private VBox createItemSlot(Item item, int index) {
        VBox slot = new VBox(5);
        slot.setAlignment(Pos.CENTER);
        slot.getStyleClass().add("item-slot");
        slot.getStyleClass().add("filled");
        slot.setPrefSize(80, 100);
        slot.setMaxSize(80, 100);
        slot.setMinSize(80, 100);
        
        // 아이템 아이콘
        Label icon = new Label(item.getIcon());
        icon.getStyleClass().add("item-icon");
        icon.setStyle("-fx-font-size: 32px;");
        
        // 아이템 이름
        Label name = new Label(item.getType().getDisplayName());
        name.getStyleClass().add("item-name");
        name.setStyle("-fx-font-size: 10px; -fx-text-fill: #fff;");
        name.setWrapText(true);
        name.setMaxWidth(75);
        
        // 사용 버튼
        Button useButton = new Button("사용");
        useButton.getStyleClass().add("item-use-button");
        useButton.setStyle("-fx-font-size: 10px; -fx-padding: 2 8 2 8;");
        useButton.setOnAction(e -> {
            if (onItemUse != null) {
                onItemUse.onUse(item, index);
            }
        });
        
        // 키보드 단축키 표시 (1, 2, 3)
        Label keyHint = new Label("[" + (index + 1) + "]");
        keyHint.getStyleClass().add("key-hint");
        keyHint.setStyle("-fx-font-size: 9px; -fx-text-fill: #aaa;");
        
        slot.getChildren().addAll(icon, name, useButton, keyHint);
        
        return slot;
    }
    
    /**
     * 아이템 추가
     * 
     * @param item 추가할 아이템
     * @return 성공 여부
     */
    public boolean addItem(Item item) {
        System.out.println("🔧 [ItemInventory] addItem called - item: " + (item != null ? item.getType() : "null"));
        System.out.println("   - inventory: " + inventory);
        System.out.println("   - inventory.size(): " + inventory.size());
        System.out.println("   - maxSize: " + maxSize);
        
        if (inventory == null) {
            System.err.println("❌ [ItemInventory] ERROR: inventory is null!");
            return false;
        }
        
        if (inventory.size() >= maxSize) {
            System.out.println("⚠️ [ItemInventory] Inventory is full!");
            return false;
        }
        
        inventory.add(item);
        System.out.println("   - After add, inventory.size(): " + inventory.size());
        
        javafx.application.Platform.runLater(() -> {
            updateUI();
            System.out.println("✅ [ItemInventory] UI updated on JavaFX thread");
        });
        
        System.out.println("✅ [ItemInventory] Item added: " + item.getType() + 
            " (" + inventory.size() + "/" + maxSize + ")");
        return true;
    }
    
    /**
     * 아이템 제거
     * 
     * @param index 제거할 슬롯 인덱스
     * @return 제거된 아이템 (없으면 null)
     */
    public Item removeItem(int index) {
        if (index < 0 || index >= inventory.size()) {
            System.out.println("⚠️ [ItemInventory] Invalid index: " + index);
            return null;
        }
        
        Item item = inventory.remove(index);
        updateUI();
        
        System.out.println("✅ [ItemInventory] Item removed: " + item.getType() + 
            " (" + inventory.size() + "/" + maxSize + ")");
        return item;
    }
    
    /**
     * 아이템 가져오기
     * 
     * @param index 슬롯 인덱스
     * @return 아이템 (없으면 null)
     */
    public Item getItem(int index) {
        if (index < 0 || index >= inventory.size()) {
            return null;
        }
        return inventory.get(index);
    }
    
    /**
     * 인벤토리 크기
     * 
     * @return 현재 아이템 수
     */
    public int getInventorySize() {
        return inventory.size();
    }
    
    /**
     * 인벤토리가 가득 찼는지 확인
     * 
     * @return 가득 찼으면 true
     */
    public boolean isFull() {
        return inventory.size() >= maxSize;
    }
    
    /**
     * 인벤토리 비우기
     */
    public void clear() {
        inventory.clear();
        updateUI();
        System.out.println("🔄 [ItemInventory] Inventory cleared");
    }
    
    /**
     * 아이템 사용 콜백 설정
     * 
     * @param callback 콜백 함수
     */
    public void setOnItemUse(ItemUseCallback callback) {
        this.onItemUse = callback;
    }
    
    /**
     * UI 업데이트
     */
    private void updateUI() {
        System.out.println("🔧 [ItemInventory] updateUI called");
        System.out.println("   - inventory.size(): " + inventory.size());
        System.out.println("   - maxSize: " + maxSize);
        
        getChildren().clear();
        itemSlots.clear();
        
        // 현재 아이템 슬롯 생성
        for (int i = 0; i < inventory.size(); i++) {
            Item item = inventory.get(i);
            System.out.println("   - Creating slot for item " + i + ": " + item.getType());
            VBox slot = createItemSlot(item, i);
            itemSlots.add(slot);
            getChildren().add(slot);
        }
        
        // 빈 슬롯 생성
        for (int i = inventory.size(); i < maxSize; i++) {
            System.out.println("   - Creating empty slot " + i);
            VBox slot = createEmptySlot(i);
            itemSlots.add(slot);
            getChildren().add(slot);
        }
        
        System.out.println("✅ [ItemInventory] UI updated - " + getChildren().size() + " children");
    }
    
    /**
     * 키보드 단축키로 아이템 사용
     * 
     * @param slotNumber 슬롯 번호 (1, 2, 3)
     */
    public void useItemByKey(int slotNumber) {
        int index = slotNumber - 1;
        if (index >= 0 && index < inventory.size() && onItemUse != null) {
            Item item = inventory.get(index);
            onItemUse.onUse(item, index);
        }
    }
}
