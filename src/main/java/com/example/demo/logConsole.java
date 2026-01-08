package com.example.demo;

import java.util.Scanner;


import org.springframework.stereotype.Service;

@Service
public class logConsole {


    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        Integer i = 0;
        while (i++ < 10) {

            System.out.println("1. Wyszukaj po NIP");
            System.out.println("2. Wyszukaj po REGON");
            System.out.println("3. Wyszukaj po KRS");
            System.out.println("4. Wyjście");

            int choice;

            while (true) {
                System.out.print("Wybierz numer opcji: ");

                if (scanner.hasNextInt()) {
                    choice = scanner.nextInt();
                    break;
                } else {
                    System.out.println("Tylko liczby całkowite !");
                    scanner.next();
                }
            }

            System.out.println("Wybrałeś: " + choice);

            System.out.println("Podaj numer rejestrowy :");
            String data = String.valueOf(scanner.next());

            switch (choice) {
                case 1 :
                    GetDataFromGUS.getAlldata("Nip",data);
                case 2 :
                    GetDataFromGUS.getAlldata("REGON",data);
                case 3 :
                    GetDataFromGUS.getAlldata("KRS",data);
            }
            i--;
        }


    }


}
