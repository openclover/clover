package coverage.enums;

public enum E1 
{
    RED(10), GREEN(20), BLUE(30)
    ;
    private final int mNum;
    
    E1(int n) {
        mNum = n;
    }

    public int getNum() {
        return mNum;
    }
}
