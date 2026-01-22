import java.util.*;


public class SMS{

        public static void main(String []args){
            
            Scanner sc = new Scanner(System.in);

           Student student = new Student(sc);
           Teacher teacher = new Teacher(sc);
          
           System.out.println("Enter Login ID : ");
              String id = sc.nextLine();
              System.out.println("Enter Password : ");
                String password = sc.nextLine();
   

               boolean logIn =  Login.verify(id,password);

    while(logIn){
        
        System.out.println("    1.Add Student");
        System.out.println("    2.View Student");
        System.out.println("    3.Delete Student");
       System.out.println("    4.Add Teacher");
       System.out.println( "    5.View Teacher");
       System.out.println("    6.Delete Teacher");
       System.out.println( "    7.Exit");
       System.out.println();

        System.out.println("Enter the operation: ");

        int ch = sc.nextInt();

        sc.nextLine();
        switch (ch) {
            case 1 :
                student.add();
                break;

            case 2 :
                student.view();
                break;
                
            case 3 :
               student.delete();
                break;

             case 4 :
                teacher.add();
                break;
                
            case 5 :
                teacher.view();
                break;
            case 6 :
                teacher.delete();
                break;
            case 7:
                sc.close();
                return;
                
            default :
            System.out.println("Invalid choice!");
            System.out.println();
           
        }
}
if(!logIn){
    System.out.println("Exiting the system due to invalid credentials.");
    }
    
    
    
}}