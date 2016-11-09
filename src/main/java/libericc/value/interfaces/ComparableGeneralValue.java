package libericc.value.interfaces;

import java.util.Set;

public interface ComparableGeneralValue {
	// return -1 for possibility of less than, 0 for equality, 1 for greater than
	Set<Integer> cmpTo(ComparableGeneralValue rhs);
}
