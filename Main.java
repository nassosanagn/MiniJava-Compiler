import syntaxtree.*;
import visitor.*;

import java.util.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length < 1){
            System.err.println("Usage: java Main <inputFile>");
            System.exit(1);
        }

        FileInputStream fis = null;
        try{
            for (int file = 0; file < args.length; file++){

                fis = new FileInputStream(args[file]);
                MiniJavaParser parser = new MiniJavaParser(fis);

                Goal root = parser.Goal();
                System.out.println("\nFile: " + args[file] + "\n");

                MyVisitor dataCollector  = new MyVisitor(false);   /* Call MyVisitor for the 1st time to create the Symbol Table List */      
                MyVisitor typeCheck = new MyVisitor(true);         /* Call MyVisitor for the 2nd time to do the typechecking */
                
                root.accept(dataCollector, null);
                root.accept(typeCheck, null);

                /* Print offsets for this file => If there are no errors */
                System.out.println("-------------------- Output -------------------- \n");
                typeCheck.output();                        
                typeCheck.deleteSymbolTables();
            }
        }
        catch(ParseException ex){
            System.out.println(ex.getMessage());
        }
        catch(FileNotFoundException ex){
            System.err.println(ex.getMessage());
        }
        finally{
            try{
                if(fis != null) fis.close();
            }
            catch(IOException ex){
                System.err.println(ex.getMessage());
            }
        }
    }
}

class Function{

    String funName;
    String funType;                             /* The return type of the function */
    int numOfArgs;                              /* Τhe number of arguments in the function */
    Map<String,String> argsArray;               /* A map with function arguments as keys and argument types as values */
    Map<String,String> varArray;                /* A map with function variables as keys and variable types as values */

    public Function(String funName, String funType, int numOfArgs){

        this.funName = funName;
        this.funType = funType;
        this.numOfArgs = numOfArgs;
        this.argsArray = new LinkedHashMap<String,String>(); 
        this.varArray = new LinkedHashMap<String,String>(); 
    }
}

class Class{

    String className;
    Map <String,String> classVarArray;      /* A map with class variables as keys and variable types as values */          
    List <Function> funList;                /* A list with the class functions */

    public Class(String className){
        this.className = className;
        this.classVarArray = new LinkedHashMap<String,String>();
        this.funList = new ArrayList<Function>();  
    }
}

class SymbolTable{

    List <Class> classList;                             /* A list with all the classes */
    
    /* Create the Symbol Table */
    public SymbolTable(String className){
        this.classList = new ArrayList<Class>();
        this.classList.add(new Class(className));  
    }

    /* Add a new class on the list */
    public void enter(String className){
        this.classList.add(new Class(className));
    }

    /* Insert the variable "varName" in the class with index "currClassIndex" */
    public void insertVarInClass(String varName, String varType, int currClassIndex){
        (this.classList.get(currClassIndex)).classVarArray.put(varName, varType);        
    }

    /* Insert the function "functionName" in the class with index "currClassIndex" */
    public void insertMethodInClass(String functionName, String returnType, int numOfArgs, int currClassIndex){
        (this.classList.get(currClassIndex)).funList.add(new Function(functionName, returnType, numOfArgs));
        
    }

    /* Insert a new argument "arguName" in the function "functionName" in the class with index "currClassIndex" */
    public void inserArguInMethod(String functionName, String arguName, String arguType, int currClassIndex){
        
        int funIndex = 0;
        for (int j = 0; j < this.classList.get(currClassIndex).funList.size(); j++) 
            if (((this.classList.get(currClassIndex)).funList.get(j).funName).equals(functionName))
                funIndex = j;
            
        (this.classList.get(currClassIndex)).funList.get(funIndex).argsArray.put(arguName, arguType);   
    }

    /* Insert a new variable "varName" in the function "functionName" in the class with index "currClassIndex" */
    public void inserVarInMethod(String functionName, String varName, String varType, int currClassIndex){

        int funIndex = 0;
        for (int j = 0; j < this.classList.get(currClassIndex).funList.size(); j++)
            if (((this.classList.get(currClassIndex)).funList.get(j).funName).equals(functionName))
                funIndex = j;
            
        (this.classList.get(currClassIndex)).funList.get(funIndex).varArray.put(varName, varType);
    }

    /* Returns first occurrence of variable "varName" used inside the function "functionName" => Return null if var doesn't exist in the Symbol Table  */
    public String lookup(String varName, String functionName, int currClassIndex){
        
        String varType;
        for (int i = currClassIndex; i >= 0; i--){                               /* For every class in this symbol table */

            for (int j = 0; j < this.classList.get(i).funList.size(); j++){                 /* For every function in the class with index i */
                
                if ((this.classList.get(i).funList.get(j).funName).equals(functionName)){       /* Find the function called "functionName" */
                    
                    /* Check function's local variables to find "varName" */
                    varType =  this.classList.get(i).funList.get(j).varArray.get(varName);

                    if (varType != null)    /* If we find the variable in the map => return the variable's type */
                        return varType;

                    /* Check function's arguments to find "varName" */
                    varType =  this.classList.get(i).funList.get(j).argsArray.get(varName);

                    if (varType != null)
                        return varType;
                }
            }
            
            /* If the variable isn't in function's local variables or on function's arguments => Search the class variables */
            varType = this.classList.get(i).classVarArray.get(varName);

            if (varType != null)
                return varType;     
        }
        return null;        /* The variable called "varName" wasn't found in the Symbol Table => return null */
    }

    /* Check if class called "className" exists in this Symbol Table => return "false" if it doesn't */
    public boolean findClassName(String className){

        for (int i = this.classList.size() - 1; i >= 0; i--){                   /* For every class in the classList */
            if ((this.classList.get(i).className).equals(className))
                return true;
        }

        return false;
    }

    /* Return the number of arguments of function called "functionName" or -1 if "functionName" doesn't exist */
    public int getNumOfArguments(String functionName){

        for (int i = this.classList.size() - 1; i >= 0; i--){                      /* For every class in this symbol table */
            for (int j = 0; j < this.classList.get(i).funList.size(); j++) {            /* For every function in the class */

                if ((this.classList.get(i).funList.get(j).funName).equals(functionName))
                    return this.classList.get(i).funList.get(j).numOfArgs;
            }
        }   
        return -1;
    }

    /* Function to search for a variable "varName" in function's called "functionName" arguments and local variables */
    public String funVarReDeclaration(String varName, String functionName, int currClassIndex){     

        String type;

        for (int j = 0; j < this.classList.get(currClassIndex).funList.size(); j++){                 /* Check every function in the class with index i */
            
            if ((this.classList.get(currClassIndex).funList.get(j).funName).equals(functionName)){       /* Find the function called "functionName" */
            
                type =  this.classList.get(currClassIndex).funList.get(j).argsArray.get(varName);        /* Check function's parameters to find "varName" */

                if (type != null)
                    return type;

                type = this.classList.get(currClassIndex).funList.get(j).varArray.get(varName);

                if (type != null)
                    return type;
            }
        }
        return null;        /* The variable called "varName" wasn't found => return null */
    }

    /* Function to search for a variable "varName" only in function's called "functionName" arguments */
    public String funArguReDeclaration(String varName, String functionName, int currClassIndex){     

        for (int j = 0; j < this.classList.get(currClassIndex).funList.size(); j++){                 /* Check every function in the class with index i */
            
            if ((this.classList.get(currClassIndex).funList.get(j).funName).equals(functionName)){       /* Find the function called "functionName" */
            
                String type =  this.classList.get(currClassIndex).funList.get(j).argsArray.get(varName);        /* Check function's parameters to find "varName" */

                if (type != null)
                    return type;
            }
        }
        return null;        /* The variable called "varName" wasn't found => return null */
    }

     /* Function to search for a variable "varName" only in function's called "functionName" arguments */
     public String classVarReDeclaration(String varName, int currClassIndex){     
        return this.classList.get(currClassIndex).classVarArray.get(varName);
    }

    /* Return the class name of the class with index "currClassIndex" */
    public String getClassName(int currClassIndex){
        return this.classList.get(currClassIndex).className;
    }

    /* Search for the function called "functionName" only inside the class called "className" => Return function's type if it exists */
    public String findFunName(String functionName, String className){

        for (int i = this.classList.size() - 1 ; i >= 0; i--){                               /* For every class in this symbol table */

            if (this.classList.get(i).className.equals(className)){
                for (int j = 0; j < this.classList.get(i).funList.size(); j++){                 /* Check every function in the class with index i */
                    if ((this.classList.get(i).funList.get(j).funName).equals(functionName))       /* Find the function called "functionName" */
                        return this.classList.get(i).funList.get(j).funType;
                }
            }
        }
        return null;
    }

    /* Search for the function called "functionName" in all the classes of this Symbol Table => Return function's type if it exists */
    public String findFunName(String functionName){

        for (int i = this.classList.size() - 1 ; i >= 0; i--){                               /* For every class in this symbol table */

            for (int j = 0; j < this.classList.get(i).funList.size(); j++){                 /* Check every function in the class with index i */
                
                if ((this.classList.get(i).funList.get(j).funName).equals(functionName))       /* Find the function called "functionName" */
                    return this.classList.get(i).funList.get(j).funType;
            }
        }
        return null;
    }

    /* Check if there is a method with the same name in a parent (only for classExtendsDeclaration) => if there is it must have the same arguments and the same return type*/
    public void sameFunDefinition(String functionName, String argsList, String methodsType){
        
        for (int i = this.classList.size() - 1 ; i >= 0; i--){      /* For every class in this symbol table */

            for (int j = 0; j < this.classList.get(i).funList.size(); j++){       /* Check every function in the class with index i */

                if ((this.classList.get(i).funList.get(j).funName).equals(functionName)){  /* Find if there is a function called "functionName" */

                    String funType = this.classList.get(i).funList.get(j).funType;

                    /* Both methods should have the same return type */
                    if (methodsType.equals(funType) == false){                   /* args[y] = argumentsType */   
                        System.err.println("error: in method overriding function called: " + functionName);
                        System.exit(1);
                    }

                    /* Both arguments should have the same arguments (types and number) */
                    String[] temp = argsList.split(", |,");
                    
                    for (int x = 0; x < temp.length ; x++){     /* Check if the function arguments match */

                        String[] args = temp[x].split(" ");

                        for (int y = 0; y < args.length - 1; y+=2){
                            
                            /* args[y+1] = argumentsName */
                            String argumentsType = this.classList.get(i).funList.get(j).argsArray.get(args[y+1]);

                            /* If argument doesn't exist or if arguments type doesn't match */
                            if ((argumentsType == null) || (argumentsType.equals(args[y]) == false)){                   /* args[y] = argumentsType */   
                                System.err.println("error: in method overriding function called: " + functionName);
                                System.exit(1);
                            }
                        }
                    }
                }
            }
        }
    }

    /* Prints the Symbol Table and the offsets if there are no errors after typechecking */
    public void PrintOffsets(){

        int offset = 0;         /* offset for variables */
        int methodOffset = 0;   /* offset for functions */

        for (int i = 0; i < this.classList.size(); i++){

            System.out.println(" ------------- Class " + this.classList.get(i).className + " ------------- ");
            System.out.println(" --- Variables --- ");

            for (Map.Entry<String, String> entry : this.classList.get(i).classVarArray.entrySet()) {            /* For every variable in the class */

                if (entry.getValue().equals("String[]"))           /* Don't print the main's arguments type "String[]" */
                    continue;
                
                System.out.println(this.classList.get(i).className + "." + entry.getKey() + " : " + offset);

                if (entry.getValue().equals("boolean"))     /* Booleans are stored in 1 byte */
                    offset += 1;
                else if (entry.getValue().equals("int"))        /* Ints are stored in 4 bytes */
                    offset += 4;
                else                                /* Pointers are stored in 8 bytes */
                    offset +=8;
            }
            
            System.out.println(" --- Methods --- ");
            for (int j = 0; j < this.classList.get(i).funList.size(); j++){       /* For every function in the class */
                
                boolean flag = false;

                for (int z = 0; z < i; z++){

                    for (int k = 0; k < this.classList.get(z).funList.size(); k++){     /* For every function in parent class */

                        /* If function already exists in class parent => dont print the address again */
                        if (this.classList.get(i).funList.get(j).funName.equals(this.classList.get(z).funList.get(k).funName)){
                            flag = true;
                            break;
                        }
                    }
                    if (flag)
                        break;
                }

                if (flag)       /* Function exists in parent class => dont print => continue to the next function */
                    continue;

                System.out.println(this.classList.get(i).className + "." + this.classList.get(i).funList.get(j).funName + " : " + methodOffset);
                methodOffset+=8;    /* we consider functions as pointers => 8 bytes */
            }
        }
    }
}

class MyVisitor extends GJDepthFirst<String,String>{

    static List <SymbolTable> st = new ArrayList<SymbolTable>();     /* The list with the SymbolTables */
    int currSymbolTable;                                             /* currSymbolTable = current ST index => We have a new Symbol table everytime we have a new ClassDeclaration */
    int currClass;                                                   /* currClass = current class index inside of this ST => We have a new Class in the classList everytime we have a new ClassExtendsDeclaration */
    boolean typeCheck;                                               /* If flag typecheck == true, it's the second time we call MyVisitor to check the variables */

    /* Initialize MyVisitor variables */
    public MyVisitor(boolean typeCheck){
        this.typeCheck = typeCheck;
        this.currSymbolTable = 0;    
        this.currClass = 0;
    }

    /* Find the index of the Symbol Table that contains the class called "className" => Return -1 if it doesn't exist */
    public int findSTindex(String className){

        /* For every symbol table */         
        for (int i = 0; i < st.size(); i++){

            /* If you find the "className" return the st index */
            if (st.get(i).findClassName(className)) 
                return i;
        }
        return -1;
    }

    /* Delete the list with the symbol tables */
    public void deleteSymbolTables(){
        st.clear();
    }

    /* Function to print the offsets for every symbol table */
    public void output(){

        /* Print every symbolTable */
        for (int i = 0; i < st.size(); i++)
            st.get(i).PrintOffsets();
    }

    /* Check if a string is numeric or not */
    public boolean isNumeric(String str) { 
        try{  
            Integer.parseInt(str);  
            return true;
        } catch(NumberFormatException e){  
            return false;  
        }  
    }

    public void checkFunArguments(String functionName, ArrayList<String> argsTypeList, int currST){

        for (int i = st.get(currST).classList.size() - 1 ; i >= 0; i--){        /* For every class in this symbol table */

            for (int j = 0; j < st.get(currST).classList.get(i).funList.size(); j++){     /* Check every function in the class with index j */

                if ((st.get(currST).classList.get(i).funList.get(j).funName).equals(functionName)){   /* Find the function called "functionName" */
                
                    List<String> values = new ArrayList(st.get(currST).classList.get(i).funList.get(j).argsArray.values());

                    for (int x = 0; x < argsTypeList.size(); x++){     /* Check if the function arguments match */

                        boolean flag = false;

                        if (argsTypeList.get(x).equals(values.get(x)) == false){    /* If argument types do not match => check if it's a parent class*/

                            int stIndex = this.findSTindex(values.get(x));

                            if (stIndex != -1){     /* Check if "values.get(x)" is a className => then check if parent classes names match with the arguType*/
                                                                
                                for (int y = st.get(stIndex).classList.size() - 1 ; y >= 0; y--){
                                    
                                    if (st.get(stIndex).classList.get(y).className.equals(argsTypeList.get(x))){
                                        flag = true;
                                        break;
                                    }
                                }

                                if (flag)
                                    continue;
                            }
                                System.err.println("error: argument types do not match: " + argsTypeList.get(x) + " and " + values.get(x) + " in function: " + functionName);
                                System.exit(1);
                        }   
                    }
                }
            }
        }
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "String"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    public String visit(MainClass n, String argu) throws Exception {

        if (!typeCheck){

            String classname = n.f1.accept(this,argu);
            
            st.add(new SymbolTable(classname));                                     /* add a new Symbol Table */
            st.get(currSymbolTable).insertVarInClass(n.f11.accept(this,argu), "String[]", currClass);

            NodeListOptional varDecls = n.f14;             /* f14 VARIABLE DECLARATIONS */
            for (int i = 0; i < varDecls.size(); ++i) {
                VarDeclaration varDecl = (VarDeclaration) varDecls.elementAt(i);
                String varId = varDecl.f1.accept(this,argu);
                String varType = varDecl.f0.accept(this,argu);

                /* Check if there is already a variable with the same name in the class */
                if (st.get(currSymbolTable).classVarReDeclaration(varId, currClass) != null){
                    System.err.println("error: class variable: " + varId + " double declaration");
                    System.exit(1);
                }

                /* Insert the class variables in this st */
                st.get(currSymbolTable).insertVarInClass(varId, varType, currClass);
            }
            
        }else{      /* If it's time for typechecking */
        
            NodeListOptional statDecls = n.f15;            /* Visit each statement */
            for (int i = 0; i < statDecls.size(); ++i) {
                Statement statDecl = (Statement) statDecls.elementAt(i);
                statDecl.f0.accept(this,"main");
            }
        }
        
        super.visit(n, argu);
        return "Main Class";
    }

    /**
    * f0 -> ClassDeclaration()
    *       | ClassExtendsDeclaration()
    */
    public String visit(TypeDeclaration n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public String visit(ClassDeclaration n, String argu) throws Exception {

        currSymbolTable++;                          /* Add a new symbol table for the new class declaration */
        String className = n.f1.accept(this,argu);

        if (!typeCheck){

            /* Check if a class called "className" already exists */
            if (this.findSTindex(className) != -1){
                System.err.println("error: Class " + className + " double Declaration");
                System.exit(1);
            }

            st.add(new SymbolTable(className));     /* Add a new Symbol Table */

            NodeListOptional varDecls = n.f3;                   /* f3 Variable Declarations */
            for (int i = 0; i < varDecls.size(); ++i) {
                VarDeclaration varDecl = (VarDeclaration) varDecls.elementAt(i);
                String varId = varDecl.f1.accept(this,argu);
                String varType = varDecl.f0.accept(this,argu);

                /* Check if there is already a variable with the same name in the class */
                if (st.get(currSymbolTable).classVarReDeclaration(varId, currClass) != null){
                    System.err.println("error: class variable: " + varId + " double declaration");
                    System.exit(1);
                }

                /* Insert the class variables in this Symbol Table */
                st.get(currSymbolTable).insertVarInClass(varId, varType, currClass);    
            }

            NodeListOptional methodDecls = n.f4;                                      
            for (int i = 0; i < methodDecls.size(); ++i){           /*  f4 Method Declarations */

                MethodDeclaration methodDecl = (MethodDeclaration) methodDecls.elementAt(i);
                String methodName = methodDecl.f2.accept(this,argu);
                String methodsType = methodDecl.f1.accept(this,argu);

                int numOfArgs;
                String argumentList = methodDecl.f4.present() ? methodDecl.f4.accept(this,argu) : "";      /* Get the function arguments */

                /* Get the number of the arguments */
                if (argumentList.isEmpty())
                    numOfArgs = 0;
                else{
                    String[] args = argumentList.split(",");
                    numOfArgs = args.length;
                }

                /* Check if there is already a function with this name in the same class => error */
                if (st.get(currSymbolTable).findFunName(methodName, className) != null){
                    System.err.println("error: function " + methodName + " double Declaration");
                    System.exit(1);
                }else
                    /* Insert this class method in this Symbol Table */
                    st.get(currSymbolTable).insertMethodInClass(methodName, methodsType, numOfArgs, currClass);    
            }
        }

        return super.visit(n, argu);
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        
        currClass++;                                    /* Add a new class in the current Symbol Table */
        String className = n.f1.accept(this,argu);

        if (!typeCheck){

            String extendsClass = n.f3.accept(this,argu);
            
            /* Find the symbolTable that contains the class called "extendsClass" that the new class called "className" extends */
            int STindex = this.findSTindex(extendsClass);
            
            if (STindex == -1){
                System.err.println("error: there is no class called: " + extendsClass);
                System.exit(1);
            }

            st.get(STindex).enter(className);    /* Add a new class in the current Symbol Table with index (STIndex) */

            NodeListOptional varDecls = n.f5;          /* f5 Variable Declarations */

            for (int i = 0; i < varDecls.size(); ++i){
                VarDeclaration varDecl = (VarDeclaration) varDecls.elementAt(i);
                String varId = varDecl.f1.accept(this,argu);
                String varType = varDecl.f0.accept(this,argu);

                /* Check if there is already a variable with the same name in the class */
                if (st.get(currSymbolTable).classVarReDeclaration(varId, currClass) != null){
                    System.err.println("error: class variable: " + varId + " double declaration");
                    System.exit(1);
                }

                /* Insert the class variables in this st */
                st.get(STindex).insertVarInClass(varId,varType,currClass);
            }

            NodeListOptional methodDecls = n.f6;               /* f6 Method Declarations */
            for (int i = 0; i < methodDecls.size(); ++i){

                MethodDeclaration methodDecl = (MethodDeclaration) methodDecls.elementAt(i);
                String methodName = methodDecl.f2.accept(this,argu);
                String methodsType = methodDecl.f1.accept(this,argu);

                int numOfArgs; 
                String argumentList = methodDecl.f4.present() ? methodDecl.f4.accept(this,argu) : "";      /* Get the function arguments */

                /* Get the number of the arguments */
                if (argumentList.isEmpty())
                    numOfArgs = 0;
                else{
                    String[] args = argumentList.split(",");
                    numOfArgs = args.length;
                }

                /* Check if there is a method with the same name in the new class => if there is it must have the same arguments and the same returj type*/
                st.get(STindex).sameFunDefinition(methodName, argumentList, methodsType);
                /* Insert this class method in this Symbol Table */
                st.get(STindex).insertMethodInClass(methodName, methodsType, numOfArgs, currClass);    
            }
        }

        super.visit(n, argu);
        return "ClassExtendsDeclaration";
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String visit(VarDeclaration n, String argu) throws Exception {
        String myType = n.f0.accept(this,argu);
        String myName = n.f1.accept(this,argu);
        return myType + " " + myName;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public String visit(MethodDeclaration n, String argu) throws Exception {
        
        String methodsType = n.f1.accept(this,argu);
        String methodName = n.f2.accept(this,argu);

        if (!typeCheck){

            String argumentList = n.f4.present() ? n.f4.accept(this,argu) : "";
            String[] temp = argumentList.split(", |,");
                        
            /* Insert function arguments in the symbol table */
            for (int i = 0; i < temp.length ; i++){
                String[] args = temp[i].split(" ");
                for  (int j = 0; j < args.length - 1; j+=2){

                    /* Check if an argument name is declared more than once */
                    if (st.get(currSymbolTable).funArguReDeclaration(args[j+1], methodName, currClass) != null){
                        System.err.println("error: double variable " + args[j+1] + " declaration in method: " + methodName);
                        System.exit(1);
                    }
                    st.get(currSymbolTable).inserArguInMethod(methodName, args[j+1], args[j], currClass);       /* args[j+1] = arguName and args[j] = arguType */
                }
            }

            /* Insert function variables in the symbol table */
            NodeListOptional varDecls = n.f7;
            for (int i = 0; i < varDecls.size(); ++i) {
                VarDeclaration varDecl = (VarDeclaration) varDecls.elementAt(i);
                String varId = varDecl.f1.accept(this,argu);
                String varType = varDecl.f0.accept(this,argu);
                
                /* Check if an method's variable name is declared more than once (in arguments or in method's body)*/
                if (st.get(currSymbolTable).funVarReDeclaration(varId, methodName, currClass) != null){
                    System.err.println("error: double variable " + varId + " declaration in method: " + methodName);
                    System.exit(1);
                }
                st.get(currSymbolTable).inserVarInMethod(methodName, varId, varType, currClass);
            }
            
        }else{      /* If it's time for typechecking */

            String returnVar = n.f10.accept(this, methodName);

            /* If expression is a number => then the function must return int */
            if (this.isNumeric(returnVar)){

                if (methodsType.equals("int") == false){
                    System.err.println("error: incompatible types: int cannot be converted to " + methodsType);
                    System.exit(1);
                }
            
            /* If expression is "true" or "false" => then the function must return boolean */
            }else if (returnVar.equals("true") || returnVar.equals("false")){

                if ((methodsType.equals("boolean") == false)){
                    System.err.println("error: incompatible types: boolean cannot be converted to " + methodsType);
                    System.exit(1);
                }

            }else{

                String typeRetVar;

                if (returnVar.equals("this"))                                        /* Return type is the className */
                    typeRetVar = st.get(currSymbolTable).getClassName(currClass);

                else if (returnVar.contains("MessageSend "))             /* If it's returning a function call then we alredy have the return type */
                    typeRetVar = returnVar.replace("MessageSend ","");

                else                                                                                /* If it's a variable get the varType */
                    typeRetVar = st.get(currSymbolTable).lookup(returnVar, methodName, currClass);
                
                /* If we have different return types */
                if ((methodsType.equals(typeRetVar) == false)){
                    System.err.println("error: incompatible return types: "+ typeRetVar + " cannot be converted to " + methodsType);
                    System.exit(1);
                }
            }

            NodeListOptional statDecls = n.f8;              /* Visit each statement */
            for (int i = 0; i < statDecls.size(); ++i) {
                Statement statDecl = (Statement) statDecls.elementAt(i);
                statDecl.f0.accept(this,methodName);
            }          
        }
        
        return "MethodDeclaration";
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterList n, String argu) throws Exception {
        String ret = n.f0.accept(this,argu);

        if (n.f1 != null) {
            ret += n.f1.accept(this,argu);
        }
        return ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    public String visit(FormalParameterTail n, String argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += ", " + node.accept(this,argu);
        }
        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
      
    public String visit(FormalParameter n, String argu) throws Exception{
        String type = n.f0.accept(this,argu);
        String name = n.f1.accept(this,argu);
        return type + " " + name;
    }

    /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    public String visit(Type n, String argu) throws Exception {
        return n.f0.accept(this,argu);
    }

    /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(ArrayType n, String argu) {
        return "int[]";
    }

    /**
    * f0 -> "boolean"
    */
    public String visit(BooleanType n, String argu) {
        return "boolean";
    }

    /**
    * f0 -> "int"
    */
    public String visit(IntegerType n, String argu) {
        return "int";
    }

    /**
    * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    */
    public String visit(Statement n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
    public String visit(Block n, String argu) throws Exception {

        if (typeCheck){

            NodeListOptional statDecls = n.f1;                                       /* f1 STATEMENTS */
            for (int i = 0; i < statDecls.size(); ++i) {
                Statement statDecl = (Statement) statDecls.elementAt(i);
                statDecl.f0.accept(this,argu);
            }
        }
        return "Block";
    }

    /**
     * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */ 
    public String visit(AssignmentStatement n, String methodName) throws Exception {

        String identifier = n.f0.accept(this,methodName);
        String expr = n.f2.accept(this,methodName);

        if (typeCheck){
            
            int stIndex = currSymbolTable;
            String idType = st.get(stIndex).lookup(identifier, methodName,currClass);
            
            /* Check if the variable before the '=' (identifier) exists in the SymbolTable (has been declared) */
            if (idType == null){                
                System.err.println("error: cannot find symbol: " + identifier + " in method: " + methodName);
                System.exit(1);
            }

            if (expr.contains("ArrayAllocationExpression ")){         /* If it's a new int[] (array allocation) */

                /* idType must be int array */
                if (idType.equals("int[]") == false){
                    System.err.println("error: incompatible types: " + idType + " cannot be converted to int[]");
                    System.exit(1);
                }

            }else if ((expr.contains("AllocationExpression"))){       /* If it's new class allocation e.g. new A() */

                String className = expr.replace("AllocationExpression", "");

                /* Check if idType and className match */
                if (idType.equals(className) == false){
                    System.err.println("error: incompatible types: " + idType + " cannot be converted to " + className);
                    System.exit(1);
                }

            }else if (expr.contains("MessageSend ")){           /* If it's a message send */

                String exprType = expr.replace("MessageSend ", "");

                /* Check if idType and exprType match */
                if (idType.equals(exprType) == false){                
                    System.err.println("error: incompatible types: " + idType + " cannot be converted to " + exprType);
                    System.exit(1);
                }

            }else{       /* If it's a variable, or a number or a PrimaryExpression e.g. x + 5 */

                String temp[] = expr.split(" ");    /* Split the expression in words */

                for (int i = 0; i < temp.length ; i++){

                    if (this.isNumeric(temp[i])){                          /* If it's a number */

                        /* Identifier must be type int */
                        if (idType.equals("int") == false){
                            System.out.println(expr);
                            System.err.println("error: incompatible types: " + idType + " cannot be converted to int");
                            System.exit(1);
                        }

                    }else{

                        /* Ignore symbols and words like "this", "true", "false" */
                        if (!(temp[i].isEmpty() ||temp[i].contains(".")  || temp[i].contains("&&") || temp[i].contains("+") || temp[i].contains("-") || temp[i].contains("*") || temp[i].contains("ArrayLookup") 
                        || temp[i].contains("length") || temp[i].contains("<") || temp[i].contains(")")|| temp[i].contains(",")|| temp[i].contains("this") || temp[i].contains("[") || temp[i].contains("true") || temp[i].contains("false"))){                      /* if temp[i] is a variable */
                            
                            String varType = st.get(stIndex).lookup(temp[i], methodName, currClass);

                            /* Check if variable exists in the Symbol Table (has been declared) */
                            if (varType == null){
                                System.err.println("error: cannot find symbol: " + temp[i]);
                                System.exit(1);
                            }

                            /* Check if idType and varType matches => or bad assignment */
                            if (idType.equals(varType) == false){
                                System.err.println("error: incompatible types: " + idType + " cannot be converted to " + varType);
                                System.exit(1);
                            }
                        }
                    }
                }
            }
        }
        return "AssignmentStatement";
    }

    /**
     * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    public String visit(ArrayAssignmentStatement n, String identifier) throws Exception {

        if (typeCheck){

            String arraysName = n.f0.accept(this,identifier);
            String index = n.f2.accept(this,identifier);
            String expr = n.f5.accept(this,identifier);
            String arraysType = st.get(currSymbolTable).lookup(arraysName, identifier, currClass);

            /* Identifier or "arraysName" must be type "int[]" */
            if (arraysType.equals("int[]") == false){
                System.err.println("error: in array assignment: array required");
                System.exit(1);
            }

            /* Check the index => must be type int*/       
            if (isNumeric(index)){                       /* If index is a number */
                /* countinue */
            }else{                                       /* If index is a variable => check variable's type */

                String indexType = st.get(currSymbolTable).lookup(index, identifier, currClass);
                
                /* Check if variable called "index" exists in the Symbol Table */
                if (indexType == null){   
                    System.err.println("error: in array assignment: cannot find symbol: " + index);
                    System.exit(1);
                }
                
                /* Ckeck if variable called "indexType" isn't type "int" */
                if (indexType.equals("int") == false){       
                    System.err.println("error: in array assignment index must be type int");
                    System.exit(1);
                }
            }

            /* Check expression => must be type int */
            if (isNumeric(expr))                       /* If index is a number */
                return "ArrayAssignmentStatement";

            else if(expr.contains("ArrayLookup") || expr.contains("+") || expr.contains("-") || expr.contains("*"))    /* If it's a PrimaryExpression it's already checked */
                return "ArrayAssignmentStatement";

            else if (expr.contains("MessageSend ")){             /* If it's a message send */

                String exprType = expr.replace("MessageSend ","");

                /* Ckeck if variable called "exprType" isn't type "int" */
                if (exprType.equals("int") == false){       
                    System.err.println("error: in array assignment expression must be type int");
                    System.exit(1);
                }

            }else{                                       /* If expr is a variable => check variable's type */

                String exprType = st.get(currSymbolTable).lookup(expr, identifier, currClass);
                
                /* Check if variable called "expr" exists in the Symbol Table */
                if (exprType == null){   
                    System.err.println("error: in array assignment: cannot find symbol: " + expr);
                    System.exit(1);
                }
                
                /* Ckeck if variable called "exprType" isn't type "int" */
                if (exprType.equals("int") == false){       
                    System.err.println("error: in array assignment expression must be type int");
                    System.exit(1);
                }
            }
        }
        return "ArrayAssignmentStatement";
    }

    /**
     * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    public String visit(IfStatement n, String argu) throws Exception {
        n.f2.accept(this,argu);
        n.f4.accept(this,argu);
        n.f6.accept(this,argu);
        return "IfStatement";
    }

    /**
     * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public String visit(WhileStatement n, String argu) throws Exception {

        n.f2.accept(this,argu);
        return n.f4.accept(this,argu);
    }

    /**
     * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String visit(PrintStatement n, String methodName) throws Exception {
        
        /* only accept expressions of type int as the argument of the PrintStatement */
        String varName = n.f2.accept(this, methodName);

        if (typeCheck){

            String varType = null;

            if (this.isNumeric(varName))                            /* It's a number = int => ok */
                return "System.out.println(" + varName + ")";
            
            else if ((varName.contains("+")) || (varName.contains("-")) || (varName.contains("*")))     /* It's a primary expression */
                return "System.out.println(" + varName + ")";

            else if (varName.contains("["))                         /* It's an index in an int array = int => ok */
                return "System.out.println(" + varName + ")";

            else if (varName.contains("MessageSend "))             /* It's a method call */
                varType = varName.replace("MessageSend ","");

            else                                                        /* It's a variable => get variable's type */
                varType = st.get(currSymbolTable).lookup(varName, methodName, currClass);
            
            /* Check if variable exists in the Symbol Table (has been declared) */
            if (varType == null){
                System.err.println("error: in print statement: cannot find symbol: " + varName);
                System.exit(1);
            }

            /* Identifier or "arraysName" must me type "int[]" */
            if (varType.equals("int") == false){
                System.err.println("error: in print statement: int required");
                System.exit(1);
            }
        }
        return "System.out.println(" + varName + ")";
    }

    /**
     * f0 -> AndExpression()
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | PrimaryExpression()
    */
    public String visit(Expression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "&&"
    * f2 -> PrimaryExpression()
    */
    public String visit(AndExpression n, String argu) throws Exception {
        return n.f0.accept(this,argu) + " && " +  n.f2.accept(this,argu);
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public String visit(CompareExpression n, String argu) throws Exception {
        return n.f0.accept(this,argu) + " < " +  n.f2.accept(this,argu);
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n, String methodName) throws Exception {
        return n.f0.accept(this,methodName) + " + " +  n.f2.accept(this,methodName);
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n, String argu) throws Exception {
        return n.f0.accept(this,argu) + " - " +  n.f2.accept(this,argu);
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public String visit(TimesExpression n, String argu) throws Exception {
        return n.f0.accept(this,argu) + " * " +  n.f2.accept(this,argu);
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n, String methodName) throws Exception {

        String arraysName = n.f0.accept(this, methodName);                                           /* Get array's name */
        String arraysType = st.get(currSymbolTable).lookup(arraysName, methodName, currClass);      /* Get array's type */

        if (typeCheck){
          
            /* Check if variable "arraysName" exists in the Symbol Table */
            if (arraysType == null){
                System.err.println("error: in array lookup: array " + arraysName + " wasn't found");
                System.exit(1);
            }

            /* Identifier or "arraysName" must be type "int[]" */
            if (arraysType.equals("int[]") == false){
                System.err.println("error: in array lookup: array required");
                System.exit(1);
            }
    
            /* The arraysIndex must be type "int" */
            String arraysIndex = n.f2.accept(this, methodName);
    
            if (arraysIndex.contains("MessageSend")){       /* If it's a method call => only keep the return type (remove "MessageSend") */

                arraysIndex = arraysIndex.replace("MessageSend", "");

                if (arraysIndex.equals("int") == false){
                    System.err.println("error: in array lookup: index must be type int");
                    System.exit(1);
                }

            }else{      /* If it's not a method call */ 
                
                if (this.isNumeric(arraysIndex) == false){      /* It's a variable => check the variable's type */

                    /* Get variable's "arraysIndexType" type */
                    String arraysIndexType = st.get(currSymbolTable).lookup(arraysIndex, methodName, currClass);

                    /* Check if variable "arraysIndex" exists in the Symbol Table */
                    if (arraysIndexType == null){
                        System.err.println("error: in array lookup: cannot find symbol " + arraysIndex);
                        System.exit(1);
                    }

                    /* Check if variable "arraysIndexType" is type "int" */
                    if (arraysIndexType.equals("int") == false){
                        System.err.println("error: in array lookup: index must be type int");
                        System.exit(1);
                    }
                }
            }
        }

        return "ArrayLookup " + n.f0.accept(this,methodName) + "[" +  n.f2.accept(this,methodName) + "]";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String visit(ArrayLength n, String methodName) throws Exception {

        String arraysName = n.f0.accept(this,methodName);
        
        if (typeCheck){
            
            String arraysType = st.get(currSymbolTable).lookup(arraysName, methodName, currClass);      /* Get array's type */

            /* Check if variable "arraysName" exists in the Symbol Table (has been declared) */
            if (arraysType == null){
                System.err.println("error in ArrayLength: array " + arraysName + " wasn't found");
                System.exit(1);
            }

            /* Identifier or "arraysName" must be type "int[]" */
            if (arraysType.equals("int[]") == false){
                System.err.println("error: in ArrayLength: array required");
                System.exit(1);
            }
        }

        return arraysName + ".length"; 
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    public String visit(MessageSend n, String methodName) throws Exception {

        /* methodName = the name of the method that this MessageSend is inside */
        String idMethod = n.f2.accept(this, methodName);

        if (typeCheck){

            String prExpr = n.f0.accept(this,methodName);
            int stIndex = currSymbolTable;                                 /* the symbol table index to search */
            String className;
            String funType;
            
            if (prExpr.contains("AllocationExpression")){           /* e.g new A() */

                className = prExpr.replace("AllocationExpression", "");      /* Remove the keyword "AllocationExpression" from the string to keep only the class name */

            }else if (prExpr.contains("(MessageSend ")){                    /* e.g. (p_node.GetLeft()).GetKey(); messageSend.messagesend */

                prExpr = prExpr.replace("(MessageSend ", "");
                className = prExpr.replace(")", "");
                    
            }else{

                if (prExpr.equals("this"))
                    className = st.get(stIndex).getClassName(currClass);
                else
                    className = st.get(stIndex).lookup(prExpr, methodName, currClass);       /* If prExpr is a variable => variable's type is a className e.g. Tree r => Tree is a varType and a className */

                if (className == null){
                    System.err.println("error: cannot find symbol: " + prExpr + " inside the function called: " + methodName);
                    System.exit(1);
                }

            }
            
            stIndex = this.findSTindex(className);    // varType == className

            /* Check if className exists in the Symbol Table */
            if (stIndex == -1){
                System.err.println("error: cannot find class: " + className);
                System.exit(1);
            }

            /* Get the function's type */
            funType = st.get(stIndex).findFunName(idMethod);

            /* Check if there is a function called "idMethod" in this class or in a parent class */
            if (funType == null){
                System.err.println("erorr: cannot find method: " + idMethod + " in class: " + className);
                System.exit(1);
            }
            
            int numOfArgs; 
            String expressionList = n.f4.present() ? n.f4.accept(this, methodName) : "";      /* Get the function arguments */
            
            if (expressionList.isEmpty())            /* Get the number of arguments in the function call */
                numOfArgs = 0;
            else{
                String[] args = expressionList.split(", |,");
                numOfArgs = args.length;

                ArrayList<String> argsTypes = new ArrayList<String>();
    
                for (int x = 0; x < args.length ; x++){         /* Get the type of each argument in an array called argsTypes */

                    if (args[x].endsWith(" "))
                        args[x] = args[x].substring(0,args[x].length() - 1);
                    
                    if (this.isNumeric(args[x]) || args[x].contains("+") || (args[x].contains("-")) || (args[x].contains("*"))){
                        argsTypes.add("int");
                        continue;
                    }

                    if (args[x].equals("true") || args[x].equals("false")){
                        argsTypes.add("boolean");
                        continue;
                    }
                    
                    if (args[x].contains("MessageSend ")){
                        args[x] = args[x].replace("MessageSend ", "");
                        argsTypes.add(args[x]);
                        continue;
                    }
                    
                    if (args[x].contains("this")){
                        argsTypes.add(st.get(currSymbolTable).getClassName(currClass));
                        continue;
                    }

                    /* If argument is a variable get argumen't type */
                    String arguType = st.get(currSymbolTable).lookup(args[x], methodName, currClass);
                    
                    /* Check if argument exists in the Symbol Table (has been declared) */
                    if (arguType == null){
                        System.err.println("error: cannot find symbol: " + args[x] + " in method: " + methodName);
                        System.exit(1);
                    }

                    argsTypes.add(arguType);
                }

                this.checkFunArguments(idMethod, argsTypes, stIndex);
            }

            /* Check if we have the same number of arguments in function definition and function call */
            if (numOfArgs != st.get(stIndex).getNumOfArguments(idMethod)){
                System.err.println("erorr: actual and formal argument lists differ in length required: " + numOfArgs + " found: " + st.get(stIndex).getNumOfArguments(idMethod));
                System.exit(1);
            }
            
            return "MessageSend " + funType;
        }
        return " ";
    }


    /**
     * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public String visit(ExpressionList n, String argu) throws Exception {

        String ret = n.f0.accept(this, argu);
        return ret + " " + n.f1.accept(this, argu);
    }

    /**
     * f0 -> ( ExpressionTerm() )*
    */     
    public String visit(ExpressionTail n, String argu) throws Exception {

        String ret = "";
        NodeListOptional varDecls = n.f0;

        for (int i = 0; i < varDecls.size(); ++i) {
            ExpressionTerm varDecl = (ExpressionTerm) varDecls.elementAt(i);
            ret = ret + ", " + varDecl.f1.accept(this,argu);
        }
        return ret;
    }

    /**
     * f0 -> ","
    * f1 -> Expression()
    */
    public String visit(ExpressionTerm n, String argu) throws Exception {
        return n.f1.accept(this,argu);
    }

    /**
     * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | NotExpression()
    *       | BracketExpression()
    */
    public String visit(PrimaryExpression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n, String argu) throws Exception {
        return n.f0.toString();    
    }

    /**
     * f0 -> "true"
    */
    public String visit(TrueLiteral n, String argu) throws Exception {
        return "true";    
    }

    /**
     * f0 -> "false"
    */
    public String visit(FalseLiteral n, String argu) throws Exception {
        return "false";    
    }
   
    /**
    * f0 -> <IDENTIFIER>
    */ 
    public String visit(Identifier n, String argu) {
        return n.f0.toString();   
    }

    /**
    * f0 -> "this"
    */ 
    public String visit(ThisExpression n, String argu) throws Exception {
        return "this";
    }

    /**
     * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(ArrayAllocationExpression n, String methodName) throws Exception {

        String expr = n.f3.accept(this,methodName);

        if (typeCheck){

            /* expr must be type int (it's an index) */
            if (isNumeric(expr))                            /* If index is a number */
                return "ArrayAllocationExpression " + expr;

            else if (expr.contains("+") || expr.contains("-") || expr.contains("*")){    /* If it's a PrimaryExpression => it's already checked */
                return "ArrayAllocationExpression " + expr;
            
            }else if (expr.contains("MessageSend ")){    /* If it's a PrimaryExpression => it's already checked */
                expr = expr.replace("MessageSend ", "");

                if (expr.equals("int") == false){
                    System.err.println("error: in array assignment expression must be type int");
                    System.exit(1);
                }

            }else{                                       /* If expr is a variable => check variable's type */

                String exprType = st.get(currSymbolTable).lookup(expr, methodName, currClass);
                
                /* Check if variable called "expr" exists in the Symbol Table */
                if (exprType == null){   
                    System.err.println("error: in array assignment: cannot find symbol: " + expr);
                    System.exit(1);
                }
                
                /* Ckeck if variable called "exprType" isn't type "int" */
                if (exprType.equals("int") == false){       
                    System.err.println("error: in array assignment expression must be type int");
                    System.exit(1);
                }
            }
        }
        return "ArrayAllocationExpression " + "expr";
    }

    /**
     * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n, String argu) throws Exception {

        String className = n.f1.accept(this, argu);

        if (typeCheck){

            int stIndex = this.findSTindex(className);
            
            /* Check if Identifier (className) exists (has been declared) */
            if (stIndex == -1){
                System.err.println("error: cannot find class: " + className);
                System.exit(1);
            }
        }

        return "AllocationExpression" + className;
    }

    /**
     * f0 -> "!"
    * f1 -> PrimaryExpression()
    */
    public String visit(NotExpression n, String argu) throws Exception {
        return n.f1.accept(this,argu);
    }

    /**
     * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public String visit(BracketExpression n, String argu) throws Exception {
        return "(" + n.f1.accept(this,argu) + ")";
    }
}