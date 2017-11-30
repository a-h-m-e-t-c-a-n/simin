package ahmetcan.simin.Api;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "transcript_list")
public class TranscriptList {
    @Attribute(name="docid")
    public String docid;
    @ElementList(entry = "track",inline = true,required = false)
    public List<Track> tracks;
}

