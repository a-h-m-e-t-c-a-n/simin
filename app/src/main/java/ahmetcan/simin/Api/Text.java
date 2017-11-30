package ahmetcan.simin.Api;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "text")
public class Text {
    @Attribute(name = "start", required = false)
    public double start;
    @Attribute(name = "dur", required = false)
    public double duration;
    @org.simpleframework.xml.Text(required = false)
    public String sentence;
}
