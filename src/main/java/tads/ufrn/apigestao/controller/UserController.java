package tads.ufrn.apigestao.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tads.ufrn.apigestao.controller.mapper.UserMapper;
import tads.ufrn.apigestao.domain.dto.user.UpdatePasswordDTO;
import tads.ufrn.apigestao.domain.dto.user.UpsertUserDTO;
import tads.ufrn.apigestao.domain.dto.user.UserDTO;
import tads.ufrn.apigestao.service.UserService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
public class UserController {

    private final UserService service;

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<UserDTO>> findAll() {
        return ResponseEntity.ok().body(service.allUsers().stream().map(UserMapper::mapper).toList());
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok().body(UserMapper.mapper(service.findUserById(id)));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @PostMapping
    public ResponseEntity<UpsertUserDTO> store(@RequestBody UpsertUserDTO model){
        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}").buildAndExpand(service.store(model).getId()).toUri();
        return ResponseEntity.created(uri).build();
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @PostMapping("/{id}/update/password")
    public ResponseEntity<Void> updatePassword(@PathVariable Long id, @RequestBody UpdatePasswordDTO dto) {
        service.updatePassword(id, dto);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @DeleteMapping("{id}/delete")
    public ResponseEntity<UserDTO> deleteById(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}
