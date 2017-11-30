package ahmetcan.simin.Api;


import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "transcript")
public class Transcript {
    @ElementList(entry = "text",inline = true)
    public List<Text> texts;
}
