/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#ifndef SHARE_OPTO_OUTPUT_HPP
#define SHARE_OPTO_OUTPUT_HPP

#include "code/debugInfo.hpp"
#include "code/exceptionHandlerTable.hpp"
#include "metaprogramming/enableIf.hpp"
#include "opto/ad.hpp"
#include "opto/c2_CodeStubs.hpp"
#include "opto/constantTable.hpp"
#include "opto/phase.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

class AbstractCompiler;
class Arena;
class Bundle;
class Block;
class Block_Array;
class C2_MacroAssembler;
class ciMethod;
class Compile;
class MachNode;
class MachSafePointNode;
class Node;
class PhaseCFG;
#ifndef PRODUCT
#define DEBUG_ARG(x) , x
#else
#define DEBUG_ARG(x)
#endif

class BufferSizingData {
public:
  int _stub;
  int _code;
  int _const;
  int _reloc;

  BufferSizingData() :
    _stub(0),
    _code(0),
    _const(0),
    _reloc(0)
  { };
};

class PhaseOutput : public Phase {
private:
  // Instruction bits passed off to the VM
  CodeBuffer             _code_buffer;           // Where the code is assembled
  int                    _first_block_size;      // Size of unvalidated entry point code / OSR poison code
  ExceptionHandlerTable  _handler_table;         // Table of native-code exception handlers
  ImplicitExceptionTable _inc_table;             // Table of implicit null checks in native code
  C2CodeStubList          _stub_list;            // List of code stubs
  OopMapSet*             _oop_map_set;           // Table of oop maps (one for each safepoint location)
  BufferBlob*            _scratch_buffer_blob;   // For temporary code buffers.
  relocInfo*             _scratch_locs_memory;   // For temporary code buffers.
  int                    _scratch_const_size;    // For temporary code buffers.
  bool                   _in_scratch_emit_size;  // true when in scratch_emit_size.

  int                    _frame_slots;           // Size of total frame in stack slots
  CodeOffsets            _code_offsets;          // Offsets into the code for various interesting entries

  uint                   _node_bundling_limit;
  Bundle*                _node_bundling_base;    // Information for instruction bundling

  // For deopt
  int                    _orig_pc_slot;
  int                    _orig_pc_slot_offset_in_bytes;

  ConstantTable          _constant_table;        // The constant table for this compilation unit.

  BufferSizingData       _buf_sizes;
  Block*                 _block;
  uint                   _index;

  void perform_mach_node_analysis();
  void pd_perform_mach_node_analysis();

public:
  PhaseOutput();
  ~PhaseOutput();

  // Convert Nodes to instruction bits and pass off to the VM
  void Output();
  bool need_stack_bang(int frame_size_in_bytes) const;
  bool need_register_stack_bang() const;
  void compute_loop_first_inst_sizes();

  void install_code(ciMethod*         target,
                    int               entry_bci,
                    AbstractCompiler* compiler,
                    bool              has_unsafe_access,
                    bool              has_wide_vectors);

  void install_stub(const char* stub_name);

  // Constant table
  ConstantTable& constant_table() { return _constant_table; }

  // Code stubs list
  void add_stub(C2CodeStub* stub) { _stub_list.add_stub(stub); }

  // Code emission iterator
  Block* block()   { return _block; }
  int index()      { return _index; }

  // The architecture description provides short branch variants for some long
  // branch instructions. Replace eligible long branches with short branches.
  void shorten_branches(uint* blk_starts);
  // If "objs" contains an ObjectValue whose id is "id", returns it, else null.
  static ObjectValue* sv_for_node_id(GrowableArray<ScopeValue*> *objs, int id);
  static void set_sv_for_object_node(GrowableArray<ScopeValue*> *objs, ObjectValue* sv);
  void FillLocArray( int idx, MachSafePointNode* sfpt, Node *local,
                     GrowableArray<ScopeValue*> *array,
                     GrowableArray<ScopeValue*> *objs );

  void Process_OopMap_Node(MachNode *mach, int current_offset);

  // Initialize code buffer
  void estimate_buffer_size(int& const_req);
  CodeBuffer* init_buffer();

  // Write out basic block data to code buffer
  void fill_buffer(C2_MacroAssembler* masm, uint* blk_starts);

  // Compute the information for the exception tables
  void FillExceptionTables(uint cnt, uint *call_returns, uint *inct_starts, Label *blk_labels);

  // Perform instruction scheduling and bundling over the sequence of
  // instructions in backwards order.
  void ScheduleAndBundle();

  void install();

  // Instruction bits passed off to the VM
  CodeBuffer*       code_buffer()               { return &_code_buffer; }
  int               first_block_size()          { return _first_block_size; }
  void              set_frame_complete(int off) { if (!in_scratch_emit_size()) { _code_offsets.set_value(CodeOffsets::Frame_Complete, off); } }
  ExceptionHandlerTable*  handler_table()       { return &_handler_table; }
  ImplicitExceptionTable* inc_table()           { return &_inc_table; }
  OopMapSet*        oop_map_set()               { return _oop_map_set; }

  // Scratch buffer
  BufferBlob*       scratch_buffer_blob()       { return _scratch_buffer_blob; }
  void         init_scratch_buffer_blob(int const_size);
  void        clear_scratch_buffer_blob();
  void          set_scratch_buffer_blob(BufferBlob* b) { _scratch_buffer_blob = b; }
  relocInfo*        scratch_locs_memory()       { return _scratch_locs_memory; }
  void          set_scratch_locs_memory(relocInfo* b)  { _scratch_locs_memory = b; }
  int               scratch_buffer_code_size()  { return (address)scratch_locs_memory() - _scratch_buffer_blob->content_begin(); }

  // emit to scratch blob, report resulting size
  uint              scratch_emit_size(const Node* n);
  void       set_in_scratch_emit_size(bool x)   {        _in_scratch_emit_size = x; }
  bool           in_scratch_emit_size() const   { return _in_scratch_emit_size;     }

  BufferSizingData* buffer_sizing_data()        { return &_buf_sizes; }

  enum ScratchBufferBlob {
    MAX_inst_size       = 2048,
    MAX_locs_size       = 128, // number of relocInfo elements
    MAX_const_size      = 128,
    MAX_stubs_size      = 128
  };

  int               frame_slots() const         { return _frame_slots; }
  int               frame_size_in_words() const; // frame_slots in units of the polymorphic 'words'
  int               frame_size_in_bytes() const { return _frame_slots << LogBytesPerInt; }

  int               bang_size_in_bytes() const;

  void          set_node_bundling_limit(uint n) { _node_bundling_limit = n; }
  void          set_node_bundling_base(Bundle* b) { _node_bundling_base = b; }

  Bundle* node_bundling(const Node *n);
  bool valid_bundle_info(const Node *n);

  bool starts_bundle(const Node *n) const;
  bool contains_as_owner(GrowableArray<MonitorValue*> *monarray, ObjectValue *ov) const;
  bool contains_as_scalarized_obj(JVMState* jvms, MachSafePointNode* sfn,
                                  GrowableArray<ScopeValue*>* objs,
                                  ObjectValue* ov) const;

  // Dump formatted assembly
#if defined(SUPPORT_OPTO_ASSEMBLY)
  void dump_asm_on(outputStream* ost, int* pcs, uint pc_limit);
#else
  void dump_asm_on(outputStream* ost, int* pcs, uint pc_limit) { return; }
#endif

  // Build OopMaps for each GC point
  void BuildOopMaps();

#ifndef PRODUCT
  void print_scheduling(outputStream* output_stream);
  void print_scheduling(); // to tty for debugging
  static void print_statistics();
#endif
};

#endif // SHARE_OPTO_OUTPUT_HPP
