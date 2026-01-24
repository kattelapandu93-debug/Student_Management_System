public class Login {
    static boolean verify(String id , String password){
        if(id.equals("pandu") && password.equals("6969")){
            System.out.println("Login Successful!\n");
            return true;
        }
        else{
            System.out.println("Invalid Credentials!\n");
            return false;
        }
    }
}
