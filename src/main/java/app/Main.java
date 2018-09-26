package app.pck;

public final class Main {


    public static void main(String[] args) {
        Main main= new Main();
        main.abba();
    }

    void abba(){
        System.out.println(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
    }
}
