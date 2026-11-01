import com.josjis.tv.FramexLinks;
public final class FramexLinksTest {
    private static void check(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError("Expected="+expected+"; actual="+actual);
    }
    public static void main(String[] args) {
        check("https://framextv.xyz/embed/550?autoplay=1&muted=0&controls=0",
                FramexLinks.embed(550,"movie",1,1,false));
        check("https://framextv.xyz/embed/1396/2/4?autoplay=1&muted=0&controls=1",
                FramexLinks.embed(1396,"tv",2,4,true));
        boolean rejected=false;
        try { FramexLinks.embed(-9,"movie",1,1,false); } catch(IllegalArgumentException e) { rejected=true; }
        if(!rejected) throw new AssertionError("Invalid TMDB ID was not rejected");
        System.out.println("FrameXTV embed URL tests: 3 passed");
    }
}
