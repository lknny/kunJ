package kunj.com.app;

import kunj.com.annotation.Access;
import kunj.com.annotation.Inject;
import kunj.com.annotation.Service;

@Service
public class College {
	@Inject
	Department cs;
	
	@Access
	public void hi() {
	System.out.println("This is college center.");
		cs.hi();
	}

}
