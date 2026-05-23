package com.example.ac2;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ac2.modelos.ModeloFilme;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    EditText edtNome, edtAno, edtNota;
    Spinner spinnerTipo, spinnerGenero, spinnerFiltro;
    CheckBox checkAssistido;
    Button btnSalvar;
    ListView listView;

    FirebaseFirestore db;

    ListenerRegistration listenerRegistration;

    ArrayList<ModeloFilme> lista;
    ArrayAdapter<String> adapter;
    String idSelecionado = null;

    boolean filtroInicializado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtNome     = findViewById(R.id.edtNome);
        edtAno      = findViewById(R.id.edtAno);
        edtNota     = findViewById(R.id.edtNota);
        spinnerTipo   = findViewById(R.id.spinnerTipo);
        spinnerGenero = findViewById(R.id.spinnerGenero);
        spinnerFiltro = findViewById(R.id.spinnerFiltroTipo);
        checkAssistido = findViewById(R.id.checkAssistido);
        btnSalvar   = findViewById(R.id.btnSalvar);
        listView    = findViewById(R.id.listViewFilmes);

        db = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "filmes");

        ArrayAdapter<CharSequence> adapterTipo =
                ArrayAdapter.createFromResource(this, R.array.tipos, android.R.layout.simple_spinner_item);
        adapterTipo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipo.setAdapter(adapterTipo);

        ArrayAdapter<CharSequence> adapterGenero =
                ArrayAdapter.createFromResource(this, R.array.generos, android.R.layout.simple_spinner_item);
        adapterGenero.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGenero.setAdapter(adapterGenero);

        ArrayAdapter<CharSequence> adapterFiltro =
                ArrayAdapter.createFromResource(this, R.array.tipos_filtro, android.R.layout.simple_spinner_item);
        adapterFiltro.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiltro.setAdapter(adapterFiltro);

        lista   = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            ModeloFilme f = lista.get(position);
            idSelecionado = f.getId();

            edtNome.setText(f.getNome());
            edtAno.setText(String.valueOf(f.getAno()));
            edtNota.setText(String.valueOf(f.getNota()));
            checkAssistido.setChecked(f.isAssistido());

            setSpinnerByValue(spinnerTipo,   adapterTipo,   f.getTipo());
            setSpinnerByValue(spinnerGenero, adapterGenero, f.getGenero());

            Toast.makeText(this, "Editando: " + f.getNome(), Toast.LENGTH_SHORT).show();
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            ModeloFilme f = lista.get(position);
            db.collection("filmes_series")
                    .document(f.getId())
                    .delete()
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Excluído!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Erro ao excluir: " + e.getMessage(), Toast.LENGTH_LONG).show());
            return true;
        });

        btnSalvar.setOnClickListener(v -> {
            String nome    = edtNome.getText().toString().trim();
            String tipo    = spinnerTipo.getSelectedItem().toString();
            String genero  = spinnerGenero.getSelectedItem().toString();
            String anoStr  = edtAno.getText().toString().trim();
            String notaStr = edtNota.getText().toString().trim();

            if (nome.isEmpty()) {
                Toast.makeText(this, "Informe o nome", Toast.LENGTH_SHORT).show();
                return;
            }
            if (tipo.equals("Selecione o tipo")) {
                Toast.makeText(this, "Selecione o tipo", Toast.LENGTH_SHORT).show();
                return;
            }
            if (genero.equals("Selecione o gênero")) {
                Toast.makeText(this, "Selecione o gênero", Toast.LENGTH_SHORT).show();
                return;
            }
            if (anoStr.isEmpty()) {
                Toast.makeText(this, "Informe o ano", Toast.LENGTH_SHORT).show();
                return;
            }
            if (notaStr.isEmpty()) {
                Toast.makeText(this, "Informe a nota", Toast.LENGTH_SHORT).show();
                return;
            }

            int    ano      = Integer.parseInt(anoStr);
            double nota     = Double.parseDouble(notaStr);
            boolean assistido = checkAssistido.isChecked();

            ModeloFilme filme = new ModeloFilme(null, nome, tipo, genero, ano, nota, assistido);

            if (idSelecionado != null) {
                db.collection("filmes_series")
                        .document(idSelecionado)
                        .set(filme)
                        .addOnSuccessListener(doc -> {
                            Toast.makeText(this, "Atualizado!", Toast.LENGTH_SHORT).show();
                            limparCampos();
                            idSelecionado = null;

                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } else {

                db.collection("filmes_series")
                        .add(filme)
                        .addOnSuccessListener(doc -> {
                            Toast.makeText(this, "Salvo com sucesso!", Toast.LENGTH_SHORT).show();
                            limparCampos();
                            // SnapshotListener atualiza a lista automaticamente
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });

        spinnerFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (!filtroInicializado) {
                    filtroInicializado = true;
                    return;
                }
                String filtro = spinnerFiltro.getSelectedItem().toString();
                carregarDados(filtro);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        carregarDados("Todos");
    }

    private void setSpinnerByValue(Spinner spinner, ArrayAdapter<CharSequence> adapter, String value) {
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void limparCampos() {
        edtNome.setText("");
        edtAno.setText("");
        edtNota.setText("");
        spinnerTipo.setSelection(0);
        spinnerGenero.setSelection(0);
        checkAssistido.setChecked(false);
        idSelecionado = null;
    }

    private void carregarDados(String filtro) {

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        listenerRegistration = db.collection("filmes_series")
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Toast.makeText(this, "Erro ao carregar: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    lista.clear();
                    ArrayList<String> textos = new ArrayList<>();

                    if (value != null) {
                        for (DocumentSnapshot doc : value) {
                            ModeloFilme f = doc.toObject(ModeloFilme.class);
                            if (f == null) continue;
                            f.setId(doc.getId());

                            if (filtro.equals("Todos") || f.getTipo().equals(filtro)) {
                                textos.add(f.getNome()
                                        + " | " + f.getTipo()
                                        + " | " + f.getGenero()
                                        + " | " + f.getAno()
                                        + " | Nota: " + f.getNota());
                                lista.add(f);
                            }
                        }
                    }

                    adapter.clear();
                    adapter.addAll(textos);
                    adapter.notifyDataSetChanged();
                });
    }
}