#include "../Codes/AMD/svds.h"
#include <string.h>
#include <stdio.h>

void matrix_write(mat *RM, char *file) {
    FILE *fid;
    int i,j;
    double val;
    
    fid = fopen(file, "w");
    fprintf(fid, "%d %d\n", RM->nrows, RM->ncols);
    for(i=0; i<RM->nrows; i++){
        for(j=0; j<RM->ncols; j++){
            val = matrix_get_element(RM, i, j);
            fprintf(fid, "%.16f  ", val);
        }
        fprintf(fid, "\n");
    }
    fclose(fid);
}

void sparse(int m, int n, int nnz)
{
    FILE* fid;
    printf("Run svds_C on sparse matrix m=%d n=%d (nnz=%d) begin.\n", m, n, nnz);
    //Read matrix in coo format from file
    fid = fopen("SNAP.dat", "r");

    mat_coo *A = coo_matrix_new(m, n, nnz);
    A->nnz = nnz;
    long long i;
    for(i=0;i<A->nnz;i++)
    {
        int ii, jj;
        double kk;
        fscanf(fid, "%d %d %lf", &ii, &jj, &kk);
        A->rows[i] = (int)ii;
        A->cols[i] = (int)jj;
        A->values[i] = kk;
    }
    fclose(fid);
    mat_csr* D = csr_matrix_new();
    
    //Convert coo format to csr format
    csr_init_from_coo(D, A);
    coo_matrix_delete(A);

    struct timeval start_timeval, end_timeval;
    
        
    int k = 100;
    
    mat *UU;
    mat *VV;
    mat *SS;
    gettimeofday(&start_timeval, NULL);
    
    //svds_C with user's options
    svds_C_opt(D, &UU, &SS, &VV, k, 1e-10, 3*k, 10);
    
    //svds_C with default options
    //svds_C(D, &UU, &SS, &VV, k);
    gettimeofday(&end_timeval, NULL);
    
    printf("  Singular values computed by svds_C:\n");
    fid = fopen("S", "w");
    fprintf(fid, "%d\n", k);
    for(i = 0; i < k; i++) {
        printf("    %.16lf\n", SS->d[i]);
        fprintf(fid, "%.16lf\n", SS->d[i]);
    }
    fclose(fid);
    printf("  The runtime of svds-C is: %f seconds\n", get_seconds_frac(start_timeval,end_timeval));
    
    matrix_write(UU,"U");
    matrix_delete(UU);
    matrix_write(VV,"V");
    matrix_delete(VV);
    matrix_delete(SS);
    csr_matrix_delete(D);
    printf("Test svds_C on sparse matrix end.\n");
}

int main(int argc, char const *argv[])
{   int m;
    int n;
    int nnz;
    
    m = atoi(argv[1]);
    n = atoi(argv[2]);
    nnz = atoi(argv[3]);
    sparse(m, n, nnz);
    return 0;
}
