package kunj.com.app;

import kunj.com.annotation.Inject;
import kunj.com.annotation.Service;

@Service
public class DepartmentOfCS implements Department {

	@Inject
	Class csClass;

	@Override
	public void hi() {
		System.out.println("This is department of Computer Science.");
		csClass.hi();

	}

}
