package simulate.z2600k.Windows98.System;

public interface Scrollable {
    void scrollUpDown(boolean up, boolean vertical);  // перемещение на одну строку вверх/вниз
    void pageUpDown(boolean up, boolean vertical);  // Page Up / Page Down. Перемещение на столько строчек, сколько сейчас мы видим (минус один).
    void handleScrollbarClick();  // так как этот элемент будет получать onOtherTouch при нажатии на ScrollBar
    void onScrollbarMoved();  // если мы двигаем ползунком, надо обновить прокрутку
}
