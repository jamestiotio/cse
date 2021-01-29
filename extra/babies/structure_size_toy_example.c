#include <stdio.h>
#include <string.h>

/*  Below structure1 and structure2 are same.
    They differ only in member's allignment */
struct structure1
{
    int id1;
    int id2;
    char name;
    char c;
    float percentage;
};

struct structure2
{
    int id1;
    char name;
    int id2;
    char c;
    float percentage;                     
};

int main()
{
    struct structure1 a;
    struct structure2 b;
    printf ("size of structure1 in bytes : %d\n",
            sizeof(a));
    printf ( "\n   Address of id1        = 0x%llx", &a.id1 );
    printf ( "\n   Address of id2        = 0x%llx", &a.id2 );
    printf ( "\n   Address of name       = 0x%llx", &a.name );
    printf ( "\n   Address of c          = 0x%llx", &a.c );
    printf ( "\n   Address of percentage = 0x%llx",
                    &a.percentage );
    printf ("   \n\nsize of structure2 in bytes : %d\n",
                    sizeof(b));
    printf ( "\n   Address of id1        = 0x%llx", &b.id1 );
    printf ( "\n   Address of name       = 0x%llx", &b.name );
    printf ( "\n   Address of id2        = 0x%llx", &b.id2 );
    printf ( "\n   Address of c          = 0x%llx", &b.c );
    printf ( "\n   Address of percentage = 0x%llx\n",
                    &b.percentage );
    return 0;
}