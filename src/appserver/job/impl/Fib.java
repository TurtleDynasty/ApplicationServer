package appserver.job.impl;

import appserver.job.Tool;

public class Fib implements Tool {

	//recursively calculates the fib number of the passed in integer
	static public Integer fib (Integer num){
		if (num == 0) {
			return 0;
		} else if (num == 1) {

			return 1;

		} else {

		    return fib(num -1) + fib(number -2);

        }
	}
	
	@Override
	public Object activate(Object parameters){
		return Fib.fib((Integer) parameters);
	}
}

