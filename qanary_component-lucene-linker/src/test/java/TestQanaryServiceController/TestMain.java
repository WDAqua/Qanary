package TestQanaryServiceController;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.springframework.util.Assert;

import eu.wdaqua.qanary.LuceneSpotter.LuceneLinker;
import eu.wdaqua.qanary.component.QanaryMessage;
import net.minidev.json.JSONObject;

public class TestMain {

	@Test
	public void test() throws URISyntaxException {
		LuceneLinker l = new  LuceneLinker();
		l.process(null);
		fail("Not yet implemented");
	}

}
