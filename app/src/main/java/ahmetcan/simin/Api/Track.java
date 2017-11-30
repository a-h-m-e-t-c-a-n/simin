package ahmetcan.simin.Api;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root
public class Track{
    @Attribute(name = "id",required = false)
    public String id;
    @Attribute(name = "name",required = false)
    public String name;
    @Attribute(name = "lang_code",required = false)
    public String langCode;
    @Attribute(name = "lang_original",required = false)
    public String langOriginal;
    @Attribute(name = "lang_translated",required = false)
    public String langTranslated;
    @Attribute(name = "lang_default",required = false)
    public String langDefault;
}