import org.jetbrains.annotations.NotNull;

/**
 * В теле класса решения разрешено использовать только финальные переменные типа RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author :TODO: Lukonin Arseny
 */
public class Solution implements MonotonicClock {
    private final RegularInt c11 = new RegularInt(0);
    private final RegularInt c12 = new RegularInt(0);
    private final RegularInt c13 = new RegularInt(0);
    private final RegularInt c21 = new RegularInt(0);
    private final RegularInt c22 = new RegularInt(0);

    @Override
    public void write(@NotNull Time time) {
        c11.setValue(time.getD1());
        c12.setValue(time.getD2());
        c13.setValue(time.getD3());
        // write right-to-left
        c22.setValue(time.getD2());
        c21.setValue(time.getD1());
    }

    @NotNull
    @Override
    public Time read() {
        int r21 = c21.getValue();
        int r22 = c22.getValue();
        int r13 = c13.getValue();
        int r12 = c12.getValue();
        int r11 = c11.getValue();
	if (r21 == r11){
		if (r22 == r12){
			return new Time(r11, r12, r13);
		}
		return new Time(r11, r12, 0);
	} 
	return new Time (r11, 0, 0);
    }
}
