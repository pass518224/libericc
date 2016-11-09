package libericc.lattice;

import heros.JoinLattice;
import libericc.value.*;

public class GeneralLattice implements JoinLattice<GeneralValue>{
	
	@Override
	public GeneralValue topElement(){
		return TopValue.v();
	}
	
	@Override
	public GeneralValue bottomElement(){
		return BottomValue.v();
	}
	
	@Override
	public GeneralValue join(GeneralValue left, GeneralValue right){
		return left.joinWith(right);
	}

}
