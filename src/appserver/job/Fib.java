package appserver.job;

public class Fib {

	//recursively calculates the fib number of the passed in integer
	static public Integer fib (Integer num){
		if (num == 0) {
			return 0;
		} else if (num == 1) {

			return 1;

		} else {

		    return fib(num -1) + fib(num -2);

        }
	}

	public Object activate(Object parameters){
		return Fib.fib((Integer) parameters);
	}
}
